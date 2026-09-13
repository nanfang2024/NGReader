package com.book.ng.domain.text

import com.book.ng.domain.parser.TextChapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TxtChapterSplitterTest {
    private fun chinese(n: Int): String {
        val digits = "零一二三四五六七八九"
        return when {
            n < 10 -> digits[n].toString()
            n == 10 -> "十"
            n < 20 -> "十" + digits[n % 10]
            else -> digits[n / 10] + "十" + if (n % 10 == 0) "" else digits[n % 10]
        }
    }

    @Test
    fun fiftyChaptersWithMixedNumbers() {
        val builder = StringBuilder()
        val titles = (1..50).map { i -> if (i % 2 == 1) "第${chinese(i)}章" else "第${i}章" }
        titles.forEachIndexed { i, title ->
            builder.append(title).append('\n')
            builder.append("这是第${i + 1}章的正文内容。\n")
        }
        val text = builder.toString()
        val chapters = TxtChapterSplitter.split(text)
        assertEquals(50, chapters.size)
        assertEquals(0, chapters.first().index)
        assertEquals(49, chapters.last().index)
        assertEquals("第一章", chapters[0].title)
        assertEquals("第2章", chapters[1].title)
        assertEquals(text.length, chapters.last().endOffset)
        chapters.zip(chapters.drop(1)).forEach { (current, next) ->
            assertEquals(current.endOffset, next.startOffset)
        }
        chapters.forEachIndexed { i, chapter ->
            assertTrue(text.substring(chapter.startOffset, chapter.endOffset).startsWith(chapter.title))
        }
    }

    @Test
    fun crlfFileSplitsCorrectly() {
        val text = "第一章 命运\r\n内容甲\r\n第二章 灾难\r\n内容乙\r\n"
        val chapters = TxtChapterSplitter.split(text)
        assertEquals(2, chapters.size)
        assertEquals("第一章 命运", chapters[0].title)
        assertEquals(0, chapters[0].startOffset)
        assertEquals(text.indexOf("第二章"), chapters[0].endOffset)
        assertEquals(text.length, chapters[1].endOffset)
    }

    @Test
    fun fallbackWhenNoChapterTitles() {
        val text = "字".repeat(7000)
        val chapters = TxtChapterSplitter.split(text)
        assertEquals(3, chapters.size)
        assertEquals(listOf("第1部分", "第2部分", "第3部分"), chapters.map { it.title })
        assertEquals(listOf(0, 3000, 6000), chapters.map { it.startOffset })
        assertEquals(listOf(3000, 6000, 7000), chapters.map { it.endOffset })
    }

    @Test
    fun fallbackWhenSingleTitleOnly() {
        val text = "第一章 三体\n短正文"
        val chapters = TxtChapterSplitter.split(text)
        assertEquals(1, chapters.size)
        assertEquals("第1部分", chapters[0].title)
        assertEquals(0, chapters[0].startOffset)
        assertEquals(text.length, chapters[0].endOffset)
    }

    @Test
    fun whitespaceAroundTitleIsTolerated() {
        val text = "   第一章\t\n正文\n\t  序言  \t\n卷首语\n\t第三章　\n正文三\n"
        val chapters = TxtChapterSplitter.split(text)
        assertEquals(3, chapters.size)
        assertEquals("第一章", chapters[0].title)
        assertEquals("序言", chapters[1].title)
        assertEquals("第三章", chapters[2].title)
    }

    @Test
    fun longTitleTruncatedToThirtyChars() {
        val title = "第一章" + "长".repeat(100)
        val text = "$title\n正文\n第二章\n正文二\n"
        val chapters = TxtChapterSplitter.split(text)
        assertEquals(2, chapters.size)
        assertEquals(30, chapters[0].title.length)
        assertEquals("第一章" + "长".repeat(27), chapters[0].title)
    }

    @Test
    fun emptyFileReturnsSingleEmptyChapter() {
        val chapters = TxtChapterSplitter.split("")
        assertEquals(1, chapters.size)
        assertEquals(TextChapter(index = 0, title = "", startOffset = 0, endOffset = 0), chapters[0])
    }
}
