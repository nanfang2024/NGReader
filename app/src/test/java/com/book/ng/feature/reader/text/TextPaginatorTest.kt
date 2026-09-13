package com.book.ng.feature.reader.text

import com.book.ng.domain.parser.TextChapter
import com.book.ng.domain.text.TxtChapterSplitter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TextPaginatorTest {
    private val viewport = Viewport(widthDp = 360, heightDp = 640, fontSizeSp = 18f)

    private fun sampleBook(): String = buildString {
        repeat(10) { i ->
            append("第").append(i + 1).append("章\n")
            repeat(80) { append("这是测试正文第").append(i + 1).append("章内容片段\n") }
        }
    }

    private fun paginate(text: String): Pair<List<TextChapter>, PagedDocument> {
        val chapters = TxtChapterSplitter.split(text)
        val document = TextPaginator(RuntimeEnvironment.getApplication())
            .paginate(text, chapters, viewport)
        return chapters to document
    }

    @Test
    fun tenThousandCharsYieldManyPages() {
        val text = sampleBook()
        assertTrue("sample should be >= 10k chars", text.length >= 10_000)
        val (chapters, document) = paginate(text)
        assertEquals(10, chapters.size)
        assertTrue("expected >10 pages but got ${document.pageCount}", document.pageCount > 10)
    }

    @Test
    fun paginationIsDeterministic() {
        val text = sampleBook()
        val (_, first) = paginate(text)
        val (_, second) = paginate(text)
        assertEquals(first.pages, second.pages)
    }

    @Test
    fun pageRangesAreContinuousWithinChapter() {
        val (_, document) = paginate(sampleBook())
        document.pages.zipWithNext { previous, next ->
            assertTrue(
                "page start must not precede its end",
                next.startOffset >= previous.endOffset,
            )
            if (previous.chapterIndex == next.chapterIndex) {
                assertEquals(
                    "intra-chapter pages must share the boundary exactly",
                    previous.endOffset,
                    next.startOffset,
                )
            }
        }
        document.pages.forEach { page ->
            assertTrue("pages must be non-empty or single-char", page.endOffset >= page.startOffset)
        }
    }

    @Test
    fun globalIndexOfFindsTheContainingPage() {
        val (_, document) = paginate(sampleBook())
        document.pages.forEachIndexed { index, page ->
            val midpoint = (page.startOffset + page.endOffset) / 2
            assertEquals(index, document.globalIndexOf(page.chapterIndex, midpoint))
            assertEquals(index, document.globalIndexOf(page.chapterIndex, page.startOffset))
        }
    }

    @Test
    fun globalIndexOfReturnsMinusOneForForeignChapter() {
        val (_, document) = paginate(sampleBook())
        val firstPage = document.pageAt(0)
        assertEquals(-1, document.globalIndexOf(firstPage.chapterIndex + 500, firstPage.startOffset))
    }

    @Test
    fun emptyChapterProducesSingleEmptyPage() {
        val text = "占位"
        val chapters = listOf(TextChapter(index = 0, title = "空", startOffset = 0, endOffset = 0))
        val document = TextPaginator(RuntimeEnvironment.getApplication())
            .paginate(text, chapters, viewport)
        assertEquals(1, document.pageCount)
        assertEquals(PageSpec(0, 0, 0), document.pageAt(0))
    }
}
