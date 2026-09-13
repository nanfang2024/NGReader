package com.book.ng.feature.reader.text

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.book.ng.data.repository.LibraryRepository
import com.book.ng.data.repository.ProgressRepository
import com.book.ng.domain.model.LibraryBook
import com.book.ng.domain.model.ReadMode
import com.book.ng.domain.model.ReadingLocator
import com.book.ng.domain.parser.TextChapter
import com.book.ng.domain.text.TxtCharsetDetector
import com.book.ng.domain.text.TxtChapterSplitter
import com.book.ng.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val title: String = "",
    val chapters: List<TextChapter> = emptyList(),
    val fullText: String = "",
    val document: PagedDocument? = null,
    val currentPage: Int = 0,
    val currentChapterIndex: Int = 0,
    val mode: ReadMode = ReadMode.PAGING,
    val fontSizeSp: Float = DEFAULT_FONT_SIZE_SP,
) {
    val pageCount: Int get() = document?.pageCount ?: 0
    val progressFraction: Float
        get() = if (pageCount == 0) 0f else (currentPage + 1f) / pageCount
    val currentChapterTitle: String
        get() = chapters.getOrNull(currentChapterIndex)?.title?.takeIf { it.isNotBlank() }
            ?: title

    companion object {
        const val DEFAULT_FONT_SIZE_SP = 18f
        const val MIN_FONT_SIZE_SP = 14f
        const val MAX_FONT_SIZE_SP = 30f
    }
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val bookId: Long = savedStateHandle[Routes.ARG_BOOK_ID] ?: -1L
    private val paginator = TextPaginator(context)
    private val density = context.resources.displayMetrics.density

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private var loaded = false
    private var viewportPx: Pair<Int, Int>? = null
    private var restoreCharOffset: Int? = null
    private var lastLocator: ReadingLocator? = null
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        viewModelScope.launch {
            runCatching { loadBook() }.onFailure {
                _state.update { it.copy(loading = false, error = "加载失败，请确认文件仍在书架中") }
            }
        }
    }

    private suspend fun loadBook() = withContext(Dispatchers.IO) {
        val book: LibraryBook = libraryRepository.books.first()
            .firstOrNull { it.id == bookId } ?: error("书籍不存在: $bookId")
        val bytes = libraryRepository.fileFor(book).readBytes()
        val charset = TxtCharsetDetector.detect(bytes)
        val text = TxtCharsetDetector.stripBom(TxtCharsetDetector.decode(bytes, charset))
        val chapters = TxtChapterSplitter.split(text)
        val locator = progressRepository.get(bookId).first()
        val mode = locator?.mode ?: ReadMode.PAGING
        val chapterIndex = if (chapters.isEmpty()) {
            0
        } else {
            locator?.chapterIndex?.coerceIn(0, chapters.lastIndex) ?: 0
        }
        _state.update {
            it.copy(
                loading = false,
                error = null,
                title = book.title,
                chapters = chapters,
                fullText = text,
                mode = mode,
                currentChapterIndex = chapterIndex,
            )
        }
        loaded = true
        if (mode == ReadMode.PAGING) {
            restoreCharOffset = locator?.scrollOffset?.takeIf { offset -> offset >= 0 }
            viewportPx?.let { repaginate(chapterIndex, restoreCharOffset) }
        }
    }

    fun onViewportSize(widthPx: Int, heightPx: Int) {
        if (widthPx <= 0 || heightPx <= 0) return
        val next = widthPx to heightPx
        if (viewportPx == next) return
        viewportPx = next
        if (!loaded || _state.value.mode != ReadMode.PAGING) return
        val s = _state.value
        val anchorChapter = s.document?.pageAt(s.currentPage)?.chapterIndex ?: s.currentChapterIndex
        val anchorOffset = s.document?.pageAt(s.currentPage)?.startOffset ?: restoreCharOffset
        viewModelScope.launch(Dispatchers.Default) { repaginate(anchorChapter, anchorOffset) }
    }

    fun onPageChange(page: Int) {
        val doc = _state.value.document ?: return
        if (doc.pageCount == 0) return
        val clamped = page.coerceIn(0, doc.pageCount - 1)
        if (clamped == _state.value.currentPage &&
            _state.value.mode == ReadMode.PAGING
        ) {
            return
        }
        val spec = doc.pageAt(clamped)
        _state.update { it.copy(currentPage = clamped, currentChapterIndex = spec.chapterIndex) }
        persist(ReadingLocator(bookId, spec.chapterIndex, clamped, spec.startOffset, ReadMode.PAGING))
    }

    fun onScrollChapterChange(chapterIndex: Int) {
        val s = _state.value
        if (s.chapters.isEmpty()) return
        val idx = chapterIndex.coerceIn(0, s.chapters.lastIndex)
        if (s.currentChapterIndex == idx) return
        _state.update { it.copy(currentChapterIndex = idx) }
        persist(
            ReadingLocator(bookId, idx, 0, s.chapters[idx].startOffset, ReadMode.SCROLL),
        )
    }

    fun onModeChange(mode: ReadMode) {
        val s = _state.value
        if (s.mode == mode || s.chapters.isEmpty()) return
        if (mode == ReadMode.SCROLL) {
            val chapterIndex = s.document?.pageAt(s.currentPage)?.chapterIndex ?: s.currentChapterIndex
            _state.update { it.copy(mode = mode, currentChapterIndex = chapterIndex) }
            persist(ReadingLocator(bookId, chapterIndex, 0, s.chapters.getOrNull(chapterIndex)?.startOffset ?: 0, mode))
        } else {
            _state.update { it.copy(mode = mode) }
            val chapterIndex = s.currentChapterIndex
            viewModelScope.launch(Dispatchers.Default) {
                repaginate(chapterIndex, s.chapters.getOrNull(chapterIndex)?.startOffset)
            }
        }
    }

    fun onFontSizeChange(delta: Int) {
        val s = _state.value
        val newSize = (s.fontSizeSp + delta)
            .coerceIn(ReaderUiState.MIN_FONT_SIZE_SP, ReaderUiState.MAX_FONT_SIZE_SP)
        if (newSize == s.fontSizeSp) return
        if (s.mode == ReadMode.SCROLL) {
            _state.update { it.copy(fontSizeSp = newSize) }
            return
        }
        _state.update { it.copy(fontSizeSp = newSize) }
        if (viewportPx == null) return
        val anchorChapter = s.document?.pageAt(s.currentPage)?.chapterIndex ?: s.currentChapterIndex
        val anchorOffset = s.document?.pageAt(s.currentPage)?.startOffset
        viewModelScope.launch(Dispatchers.Default) { repaginate(anchorChapter, anchorOffset) }
    }

    private fun repaginate(anchorChapter: Int, anchorOffset: Int?) {
        val s = _state.value
        val (widthPx, heightPx) = viewportPx ?: return
        if (s.chapters.isEmpty()) return
        val viewport = Viewport(
            widthDp = (widthPx / density).toInt(),
            heightDp = (heightPx / density).toInt(),
            fontSizeSp = s.fontSizeSp,
        )
        val doc = paginator.paginate(s.fullText, s.chapters, viewport)
        val chapter = anchorChapter.coerceIn(0, s.chapters.lastIndex)
        val page = if (anchorOffset != null && anchorOffset >= 0) {
            doc.globalIndexOf(chapter, anchorOffset).takeIf { it >= 0 }
                ?: doc.pages.indexOfFirst { it.chapterIndex == chapter }.coerceAtLeast(0)
        } else {
            doc.pages.indexOfFirst { it.chapterIndex == chapter }.coerceAtLeast(0)
        }
        restoreCharOffset = null
        val spec = doc.pageAt(page)
        _state.update { it.copy(document = doc, currentPage = page, currentChapterIndex = spec.chapterIndex) }
        persist(ReadingLocator(bookId, spec.chapterIndex, page, spec.startOffset, ReadMode.PAGING))
    }

    private fun persist(locator: ReadingLocator) {
        lastLocator = locator
        viewModelScope.launch { progressRepository.save(locator) }
    }

    override fun onCleared() {
        lastLocator?.let { locator ->
            saveScope.launch { progressRepository.save(locator) }
        }
    }
}
