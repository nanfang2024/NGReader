package com.book.ng.feature.reader.text

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.book.ng.data.db.NGBookDatabase
import com.book.ng.data.repository.LibraryRepository
import com.book.ng.data.repository.ProgressRepository
import com.book.ng.domain.model.BookFormat
import com.book.ng.ui.nav.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PagingViewportContractTest {

    @get:Rule
    val composeRule = createComposeRule()

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
    fun txtOpensIntoPagingWithViewportReportedByUi() {
        val book = runBlocking {
            libraryRepository.importFile(
                "视口契约之书.txt",
                sampleText().byteInputStream(Charsets.UTF_8),
                BookFormat.TXT,
            )
        }
        val vm = ReaderViewModel(
            savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_BOOK_ID to book.id)),
            context = context,
            libraryRepository = libraryRepository,
            progressRepository = progressRepository,
        )
        composeRule.setContent {
            MaterialTheme {
                TextReaderScreen(onBack = {}, viewModel = vm)
            }
        }
        composeRule.waitUntil(TIMEOUT_MS) { vm.state.value.document != null }
        val doc = vm.state.value.document
        assertNotNull(
            "UI 必须在无人干预时自行上报视口，否则 TXT 翻页永远停在加载占位（真机转圈缺陷）",
            doc,
        )
        assertTrue("分页结果必须至少一页", (doc?.pageCount ?: 0) > 0)
        ViewModelStore().apply { put(KEY, vm) }.clear()
    }

    private fun sampleText(): String = buildString {
        repeat(6) { i ->
            append("第${i + 1}章\n")
            repeat(80) { append("这是视口契约测试正文第${i + 1}章内容片段\n") }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val DRAIN_MS = 1_000L
        const val KEY = "viewport-contract"
    }
}
