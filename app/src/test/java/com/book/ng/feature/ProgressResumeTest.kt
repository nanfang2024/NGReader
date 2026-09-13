package com.book.ng.feature

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.book.ng.data.db.NGBookDatabase
import com.book.ng.data.parser.EpubFormatParser
import com.book.ng.data.repository.LibraryRepository
import com.book.ng.data.repository.ProgressRepository
import com.book.ng.domain.model.BookFormat
import com.book.ng.domain.model.LibraryBook
import com.book.ng.domain.model.ReadMode
import com.book.ng.domain.model.ReadingLocator
import com.book.ng.feature.reader.epub.EpubReaderViewModel
import com.book.ng.feature.reader.text.ReaderViewModel
import com.book.ng.ui.nav.Routes
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressResumeTest {

    private lateinit var context: Context
    private lateinit var database: NGBookDatabase
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var progressRepository: ProgressRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, NGBookDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        libraryRepository = LibraryRepository(context, database.bookDao())
        progressRepository = ProgressRepository(database.progressDao())
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        runBlocking { delay(DRAIN_MS) }
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun txtSavedPageTwelveIsRestoredAfterReopen() = runBlocking {
        val book = importText(sampleText())
        val vm1 = newTextVm(book.id)
        awaitUntil("TXT 首次加载") { !vm1.state.value.loading }
        assertNull(vm1.state.value.error)
        vm1.onViewportSize(WIDTH_PX, HEIGHT_PX)
        awaitUntil("TXT 分页完成") { vm1.state.value.document != null }
        val doc1 = vm1.state.value.document!!
        assertTrue("页数应大于 13，实际 ${doc1.pageCount}", doc1.pageCount > 13)

        vm1.onPageChange(12)
        assertEquals(12, vm1.state.value.currentPage)
        val spec = doc1.pageAt(12)
        awaitUntil("页 12 进度落库") {
            progressRepository.get(book.id).first()?.let { locator ->
                locator.page == 12 && locator.scrollOffset == spec.startOffset
            } == true
        }

        progressRepository.save(
            ReadingLocator(bookId = book.id, chapterIndex = 0, page = 3, scrollOffset = 0, mode = ReadMode.PAGING),
        )
        destroyViewModel(vm1)
        awaitUntil("onCleared 兜底写回页 12") {
            progressRepository.get(book.id).first()?.page == 12
        }

        val vm2 = newTextVm(book.id)
        awaitUntil("TXT 重开加载") { !vm2.state.value.loading }
        assertEquals(spec.chapterIndex, vm2.state.value.currentChapterIndex)
        vm2.onViewportSize(WIDTH_PX, HEIGHT_PX)
        awaitUntil("TXT 重开分页") { vm2.state.value.document != null }
        assertEquals(12, vm2.state.value.currentPage)
        destroyViewModel(vm2)
    }

    @Test
    fun fontSizeChangeKeepsSavedContentAnchor() = runBlocking {
        val book = importText(sampleText())
        val vm1 = newTextVm(book.id)
        awaitUntil("加载") { !vm1.state.value.loading }
        vm1.onViewportSize(WIDTH_PX, HEIGHT_PX)
        awaitUntil("分页") { vm1.state.value.document != null }
        val doc1 = vm1.state.value.document!!
        assertTrue("页数应大于 16，实际 ${doc1.pageCount}", doc1.pageCount > 16)
        vm1.onPageChange(15)
        val spec15 = doc1.pageAt(15)
        awaitUntil("页 15 落库") {
            progressRepository.get(book.id).first()?.let { locator ->
                locator.page == 15 && locator.scrollOffset == spec15.startOffset
            } == true
        }

        vm1.onFontSizeChange(2)
        vm1.onFontSizeChange(2)
        val repaginationMarker = vm1.viewModelScope.launch(Dispatchers.Default) { }
        repaginationMarker.join()
        assertEquals(22f, vm1.state.value.fontSizeSp, 0f)
        assertNull(vm1.state.value.error)
        val doc2 = vm1.state.value.document!!
        val spec2 = doc2.pageAt(vm1.state.value.currentPage)
        assertEquals(spec15.chapterIndex, spec2.chapterIndex)
        assertTrue(
            "重排页首应不越过原锚点：${spec2.startOffset} > ${spec15.startOffset}",
            spec2.startOffset <= spec15.startOffset,
        )
        awaitUntil("重排锚定落库") {
            progressRepository.get(book.id).first()?.let { locator ->
                locator.scrollOffset == spec2.startOffset && locator.chapterIndex == spec2.chapterIndex
            } == true
        }
        destroyViewModel(vm1)

        val vm2 = newTextVm(book.id)
        awaitUntil("重开加载") { !vm2.state.value.loading }
        vm2.onFontSizeChange(2)
        vm2.onFontSizeChange(2)
        vm2.onViewportSize(WIDTH_PX, HEIGHT_PX)
        vm2.viewModelScope.launch(Dispatchers.Default) { }.join()
        awaitUntil("重开分页") { vm2.state.value.document != null }
        val doc3 = vm2.state.value.document!!
        val spec3 = doc3.pageAt(vm2.state.value.currentPage)
        assertEquals(spec2.startOffset, spec3.startOffset)
        assertEquals(spec2.chapterIndex, spec3.chapterIndex)
        destroyViewModel(vm2)
    }

    @Test
    fun epubOpensAtSavedChapterAndScroll() = runBlocking {
        val source = SAMPLE_CANDIDATES.firstOrNull { it.isFile }
            ?: throw AssertionError("sample.epub 未找到：${SAMPLE_CANDIDATES.map { file -> file.absolutePath }}")
        val book = libraryRepository.importFile("测试之书.epub", source.inputStream(), BookFormat.EPUB)
        progressRepository.save(
            ReadingLocator(bookId = book.id, chapterIndex = 2, page = 0, scrollOffset = 137, mode = ReadMode.SCROLL),
        )
        val vm = EpubReaderViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_BOOK_ID to book.id)),
            libraryRepository = libraryRepository,
            progressRepository = progressRepository,
            epubParser = EpubFormatParser(context),
        )
        awaitUntil("EPUB 加载") { !vm.state.value.loading }
        val state = vm.state.value
        assertNull(state.error)
        assertEquals("测试之书", state.title)
        assertEquals(3, state.chapters.size)
        assertEquals(2, state.currentChapterIndex)
        assertEquals(137, state.restoreScrollY)
        assertTrue(state.chapterHtml.isNotBlank())
        destroyViewModel(vm)
    }

    private fun destroyViewModel(viewModel: ViewModel) {
        val store = ViewModelStore()
        store.put("resume", viewModel)
        store.clear()
    }

    private suspend fun importText(text: String): LibraryBook =
        libraryRepository.importFile("测试之书.txt", text.byteInputStream(Charsets.UTF_8), BookFormat.TXT)

    private fun newTextVm(bookId: Long) = ReaderViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_BOOK_ID to bookId)),
        context = context,
        libraryRepository = libraryRepository,
        progressRepository = progressRepository,
    )

    private suspend fun awaitUntil(what: String, condition: suspend () -> Boolean) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("等待超时：$what")
            }
            delay(25)
        }
    }

    private fun sampleText(): String = buildString {
        repeat(12) { i ->
            append("第${i + 1}章\n")
            repeat(110) { append("这是测试正文第${i + 1}章内容片段\n") }
        }
    }

    private companion object {
        const val WIDTH_PX = 360
        const val HEIGHT_PX = 640
        const val TIMEOUT_MS = 10_000L
        const val DRAIN_MS = 1_000L
        val SAMPLE_CANDIDATES = listOf(
            File("src/test/assets/sample.epub"),
            File("app/src/test/assets/sample.epub"),
        )
    }
}
