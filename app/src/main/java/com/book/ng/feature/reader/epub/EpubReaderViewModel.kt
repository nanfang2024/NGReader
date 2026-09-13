package com.book.ng.feature.reader.epub

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.book.ng.data.parser.EpubFormatParser
import com.book.ng.data.parser.EpubHandle
import com.book.ng.data.repository.LibraryRepository
import com.book.ng.data.repository.ProgressRepository
import com.book.ng.domain.model.ReadMode
import com.book.ng.domain.model.ReadingLocator
import com.book.ng.domain.parser.EpubChapter
import com.book.ng.domain.parser.ParsedBook
import com.book.ng.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EpubReaderUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val title: String = "",
    val chapters: List<EpubChapter> = emptyList(),
    val currentChapterIndex: Int = 0,
    val chapterHtml: String = "",
    val chapterBaseUrl: String = "",
    val contentVersion: Int = 0,
    val restoreScrollY: Int = 0,
) {
    val currentChapterTitle: String
        get() = chapters.getOrNull(currentChapterIndex)?.title?.takeIf { it.isNotBlank() } ?: title
}

@HiltViewModel
class EpubReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val progressRepository: ProgressRepository,
    private val epubParser: EpubFormatParser,
) : ViewModel() {

    private val bookId: Long = savedStateHandle[Routes.ARG_BOOK_ID] ?: -1L

    private val _state = MutableStateFlow(EpubReaderUiState())
    val state: StateFlow<EpubReaderUiState> = _state.asStateFlow()

    private var handle: EpubHandle? = null
    private var lastLocator: ReadingLocator? = null
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scrollReports = MutableStateFlow<Pair<Int, Int>?>(null)

    init {
        viewModelScope.launch {
            runCatching { loadBook() }.onFailure {
                _state.update { it.copy(loading = false, error = "EPUB 加载失败，请确认文件仍在书架中") }
            }
        }
        viewModelScope.launch {
            scrollReports.filterNotNull().debounce(SCROLL_DEBOUNCE_MS).collect { (chapterIndex, scrollY) ->
                persist(locatorFor(chapterIndex, scrollY))
            }
        }
    }

    private suspend fun loadBook() = withContext(Dispatchers.IO) {
        val book = libraryRepository.books.first().firstOrNull { it.id == bookId }
            ?: error("书籍不存在: $bookId")
        val opened = epubParser.open(libraryRepository.fileFor(book))
        handle = opened
        val parsed = opened.parsed
        val chapters = parsed.chapters
        val locator = progressRepository.get(bookId).first()
        val chapterIndex = if (chapters.isEmpty()) {
            0
        } else {
            locator?.chapterIndex?.coerceIn(0, chapters.lastIndex) ?: 0
        }
        val scrollY = if (locator?.mode == ReadMode.SCROLL) {
            locator.scrollOffset.takeIf { offset -> offset >= 0 } ?: 0
        } else {
            0
        }
        val html = if (chapters.isEmpty()) "" else opened.chapterHtml(chapterIndex)
        val baseUrl = opened.chapterBaseUrl(chapterIndex)
        _state.update {
            it.copy(
                loading = false,
                error = null,
                title = parsed.title,
                chapters = chapters,
                currentChapterIndex = chapterIndex,
                chapterHtml = html,
                chapterBaseUrl = baseUrl,
                contentVersion = it.contentVersion + 1,
                restoreScrollY = scrollY,
            )
        }
        lastLocator = locatorFor(chapterIndex, scrollY)
    }

    fun selectChapter(index: Int) {
        val current = _state.value
        val opened = handle ?: return
        if (index !in current.chapters.indices || index == current.currentChapterIndex) return
        viewModelScope.launch {
            runCatching {
                val html = opened.chapterHtml(index)
                val baseUrl = opened.chapterBaseUrl(index)
                _state.update {
                    it.copy(
                        currentChapterIndex = index,
                        chapterHtml = html,
                        chapterBaseUrl = baseUrl,
                        contentVersion = it.contentVersion + 1,
                        restoreScrollY = 0,
                    )
                }
                persist(locatorFor(index, 0))
            }.onFailure {
                _state.update { it.copy(error = "章节加载失败") }
            }
        }
    }

    fun onScrollReported(scrollY: Int) {
        if (scrollY < 0) return
        scrollReports.value = _state.value.currentChapterIndex to scrollY
    }

    private fun locatorFor(chapterIndex: Int, scrollY: Int): ReadingLocator =
        ReadingLocator(
            bookId = bookId,
            chapterIndex = chapterIndex,
            page = 0,
            scrollOffset = scrollY,
            mode = ReadMode.SCROLL,
        )

    private fun persist(locator: ReadingLocator) {
        lastLocator = locator
        viewModelScope.launch {
            progressRepository.save(locator)
        }
    }

    override fun onCleared() {
        handle?.close()
        handle = null
        val pending = lastLocator ?: return
        saveScope.launch {
            progressRepository.save(pending)
        }
    }

    companion object {
        private const val SCROLL_DEBOUNCE_MS = 600L
    }
}
