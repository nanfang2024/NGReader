package com.book.ng.data.parser

import android.content.Context
import com.book.ng.domain.parser.ParsedBook
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EpubParseTest {

    @Test
    fun parsesSampleEpub() {
        val context: Context = RuntimeEnvironment.getApplication()
        val source = SAMPLE_CANDIDATES.firstOrNull { it.isFile }
            ?: throw AssertionError("sample.epub 未找到：${SAMPLE_CANDIDATES.map { file -> file.absolutePath }}")
        val copy = File(context.cacheDir, "epub-sample-${System.currentTimeMillis()}.epub")
        source.copyTo(copy, overwrite = true)
        val parsed = runBlocking { EpubFormatParser(context).parse(copy) }
        assertTrue(
            "期望 ParsedBook.Epub，实际 ${parsed::class.simpleName}",
            parsed is ParsedBook.Epub,
        )
        val epub = parsed as ParsedBook.Epub
        assertEquals("测试之书", epub.title)
        assertEquals("佚名", epub.author)
        assertEquals(3, epub.chapters.size)
        assertTrue("首章标题应含“启程”，实际：${epub.chapters.first().title}", epub.chapters.first().title.contains("启程"))
        assertTrue(epub.chapters.all { chapter -> chapter.href.isNotBlank() })
        copy.delete()
    }

    private companion object {
        val SAMPLE_CANDIDATES = listOf(
            File("src/test/assets/sample.epub"),
            File("app/src/test/assets/sample.epub"),
        )
    }
}
