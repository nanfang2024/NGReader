package com.book.ng.domain.parser

import com.book.ng.domain.model.BookFormat
import java.io.File

data class TextChapter(
    val index: Int,
    val title: String,
    val startOffset: Int,
    val endOffset: Int,
)

sealed interface ParsedBook {
    data class Text(
        val title: String,
        val chapters: List<TextChapter>,
    ) : ParsedBook

    data class Comic(
        val title: String,
    ) : ParsedBook
}

interface FormatParser {
    fun supports(format: BookFormat): Boolean

    suspend fun parse(file: File): ParsedBook
}
