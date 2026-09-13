package com.book.ng.feature.reader.text

import android.content.Context
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import com.book.ng.domain.parser.TextChapter
import kotlin.math.max

data class Viewport(
    val widthDp: Int,
    val heightDp: Int,
    val fontSizeSp: Float,
    val lineSpacingMultiplier: Float = 1.2f,
    val paddingDp: Int = 16,
)

data class PageSpec(
    val chapterIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
)

data class PagedDocument(val pages: List<PageSpec>) {
    val pageCount: Int get() = pages.size

    fun pageAt(index: Int): PageSpec = pages[index]

    fun globalIndexOf(chapterIndex: Int, charOffset: Int): Int {
        if (pages.isEmpty()) return -1
        var lo = 0
        var hi = pages.lastIndex
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            val page = pages[mid]
            val notAfterTarget = page.chapterIndex < chapterIndex ||
                (page.chapterIndex == chapterIndex && page.startOffset <= charOffset)
            if (notAfterTarget) lo = mid else hi = mid - 1
        }
        val page = pages[lo]
        return if (page.chapterIndex == chapterIndex &&
            charOffset in page.startOffset..page.endOffset
        ) {
            lo
        } else {
            -1
        }
    }
}

class TextPaginator(private val context: Context) {
    fun paginate(
        text: String,
        chapters: List<TextChapter>,
        viewport: Viewport,
    ): PagedDocument {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val paddingPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            viewport.paddingDp.toFloat(),
            metrics,
        )
        val contentWidthPx = max(1, (viewport.widthDp * density - 2 * paddingPx).toInt())
        val contentHeightPx = max(1f, viewport.heightDp * density - 2 * paddingPx)
        val paint = TextPaint().apply {
            isAntiAlias = true
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                viewport.fontSizeSp,
                metrics,
            )
        }
        val pages = mutableListOf<PageSpec>()
        chapters.forEach { chapter ->
            val chapterText = text.substring(chapter.startOffset, chapter.endOffset)
            if (chapterText.isEmpty()) {
                pages += PageSpec(chapter.index, chapter.startOffset, chapter.endOffset)
                return@forEach
            }
            val layout = StaticLayout.Builder
                .obtain(chapterText, 0, chapterText.length, paint, contentWidthPx)
                .setLineSpacing(0f, viewport.lineSpacingMultiplier)
                .setIncludePad(false)
                .build()
            var pageStart = chapter.startOffset
            var usedHeight = 0f
            var previousBottom = 0
            for (line in 0 until layout.lineCount) {
                val bottom = layout.getLineBottom(line)
                val lineHeight = (bottom - previousBottom).toFloat()
                previousBottom = bottom
                if (line > 0 && usedHeight + lineHeight > contentHeightPx) {
                    val end = chapter.startOffset + layout.getLineStart(line)
                    pages += PageSpec(chapter.index, pageStart, end)
                    pageStart = end
                    usedHeight = lineHeight
                } else {
                    usedHeight += lineHeight
                }
            }
            pages += PageSpec(chapter.index, pageStart, chapter.endOffset)
        }
        return PagedDocument(pages)
    }
}
