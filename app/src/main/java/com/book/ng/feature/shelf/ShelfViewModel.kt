package com.book.ng.feature.shelf

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.book.ng.data.importer.FormatSniffer
import com.book.ng.data.repository.LibraryRepository
import com.book.ng.data.repository.ProgressRepository
import com.book.ng.domain.model.BookFormat
import com.book.ng.domain.model.LibraryBook
import com.book.ng.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ShelfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    data class ShelfBookItem(
        val book: LibraryBook,
        val progressLabel: String?,
    )

    data class ShelfUiState(
        val books: List<ShelfBookItem> = emptyList(),
        val isEmpty: Boolean = true,
        val importingMessage: String? = null,
        val snackbarMessage: String? = null,
    )

    private val importingMessage = MutableStateFlow<String?>(null)
    private val snackbarMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ShelfUiState> = combine(
        libraryRepository.books,
        progressRepository.observeAll(),
        importingMessage,
        snackbarMessage,
    ) { books, progress, importing, snackbar ->
        ShelfUiState(
            books = books.map { book ->
                ShelfBookItem(
                    book = book,
                    progressLabel = progress[book.id]?.let { locator ->
                        "第${locator.chapterIndex + 1}章 · 第${locator.page + 1}页"
                    },
                )
            },
            isEmpty = books.isEmpty(),
            importingMessage = importing,
            snackbarMessage = snackbar,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShelfUiState())

    fun import(uris: List<Uri>) {
        if (importingMessage.value != null) return
        viewModelScope.launch(Dispatchers.IO) {
            importingMessage.value = "正在导入…"
            var failures = 0
            uris.forEachIndexed { index, uri ->
                importingMessage.value = "正在导入 ${index + 1}/${uris.size}…"
                runCatching { importOne(uri) }.onFailure { failures++ }
            }
            importingMessage.value = null
            if (failures > 0) {
                snackbarMessage.value = "有 $failures 个文件导入失败"
            }
        }
    }

    private suspend fun importOne(uri: Uri): String {
        val displayName = queryDisplayName(uri)
        val temp = File(context.cacheDir, "import-${UUID.randomUUID()}")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("无法打开所选文件")
            input.use { source ->
                temp.outputStream().use { output -> source.copyTo(output) }
            }
            val format = FormatSniffer.sniff(temp)
            temp.inputStream().use { source ->
                libraryRepository.importFile(displayName, source, sniffed = format)
            }
        } finally {
            temp.delete()
        }
        return displayName
    }

    private fun queryDisplayName(uri: Uri): String {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && !cursor.isNull(index)) {
                        return cursor.getString(index)
                    }
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "未命名文件"
    }

    fun onBookClicked(item: ShelfBookItem): String? = when (item.book.format) {
        BookFormat.TXT -> Routes.readerTxt(item.book.id)
        BookFormat.EPUB -> Routes.readerEpub(item.book.id)
        else -> {
            snackbarMessage.value = "该格式将在后续阶段支持"
            null
        }
    }

    fun onSnackbarShown() {
        snackbarMessage.value = null
    }
}
