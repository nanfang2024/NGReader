package com.book.ng.domain.text

import com.book.ng.domain.parser.TextChapter

object TxtChapterSplitter {
    private val TITLE_REGEX = Regex(
        "^\\s*(第\\s*[0-9〇零一二三四五六七八九十百千万两]+\\s*[章卷回节篇集]|序言|前言|楔子|后记|尾声|附录)",
    )
    private const val FALLBACK_CHUNK_CHARS = 3000
    private const val TITLE_MAX_CHARS = 30

    fun split(text: String): List<TextChapter> {
        if (text.isEmpty()) return listOf(TextChapter(0, "", 0, 0))
        val matches = scanTitleLines(text)
        if (matches.size < 2) return fallbackChapters(text)
        return matches.mapIndexed { i, match ->
            TextChapter(
                index = i,
                title = match.title,
                startOffset = match.lineStart,
                endOffset = if (i == matches.lastIndex) text.length else matches[i + 1].lineStart,
            )
        }
    }

    private data class TitleMatch(val lineStart: Int, val title: String)

    private fun scanTitleLines(text: String): List<TitleMatch> {
        val matches = mutableListOf<TitleMatch>()
        var pos = 0
        while (true) {
            val newline = text.indexOf('\n', pos)
            val lineEnd = if (newline == -1) text.length else newline
            var contentEnd = lineEnd
            if (contentEnd > pos && text[contentEnd - 1] == '\r') contentEnd--
            val line = text.substring(pos, contentEnd)
            if (TITLE_REGEX.containsMatchIn(line)) {
                matches += TitleMatch(lineStart = pos, title = line.trim().take(TITLE_MAX_CHARS))
            }
            if (newline == -1) break
            pos = newline + 1
        }
        return matches
    }

    private fun fallbackChapters(text: String): List<TextChapter> {
        val chapters = mutableListOf<TextChapter>()
        var start = 0
        var part = 1
        while (start < text.length) {
            val end = minOf(start + FALLBACK_CHUNK_CHARS, text.length)
            chapters += TextChapter(
                index = part - 1,
                title = "第${part}部分",
                startOffset = start,
                endOffset = end,
            )
            start = end
            part++
        }
        return chapters
    }
}
