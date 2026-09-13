package com.book.ng.domain.model

enum class BookFormat {
    EPUB,
    TXT,
    MOBI,
    AZW3,
    PDF,
    CBZ,
    ZIP,
    UNKNOWN,
}

data class LibraryBook(
    val id: Long,
    val title: String,
    val author: String?,
    val fileName: String,
    val format: BookFormat,
    val addedAt: Long,
    val lastReadAt: Long?,
)

enum class ReadMode {
    PAGING,
    SCROLL,
}

data class ReadingLocator(
    val bookId: Long,
    val chapterIndex: Int,
    val page: Int,
    val scrollOffset: Int,
    val mode: ReadMode,
)
