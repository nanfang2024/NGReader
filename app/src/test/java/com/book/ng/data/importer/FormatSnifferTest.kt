package com.book.ng.data.importer

import com.book.ng.domain.model.BookFormat
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FormatSnifferTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun zip(name: String, entries: List<String>): File {
        val file = File(folder.root, name)
        ZipOutputStream(file.outputStream().buffered()).use { zos ->
            entries.forEach { entry ->
                zos.putNextEntry(ZipEntry(entry))
                zos.write("dummy-payload".toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }
        }
        return file
    }

    @Test
    fun epubDetectedByContainerXmlEvenWithImages() {
        val file = zip(
            "book.epub",
            listOf(
                "mimetype",
                "META-INF/container.xml",
                "OEBPS/content.opf",
                "OEBPS/images/cover.jpg",
            ),
        )
        assertEquals(BookFormat.EPUB, FormatSniffer.sniff(file))
    }

    @Test
    fun cbzDetectedByImageEntries() {
        val file = zip("comic.cbz", listOf("001.jpg", "002.png", "003.webp"))
        assertEquals(BookFormat.CBZ, FormatSniffer.sniff(file))
    }

    @Test
    fun plainArchiveDetectedAsZip() {
        val file = zip("archive.zip", listOf("data.txt", "notes.md"))
        assertEquals(BookFormat.ZIP, FormatSniffer.sniff(file))
    }

    @Test
    fun pdfDetectedByHeaderMagic() {
        val file = File(folder.root, "doc.pdf")
        file.writeBytes("%PDF-1.7\n%âãÏÓ\n1 0 obj\n".toByteArray(StandardCharsets.ISO_8859_1))
        assertEquals(BookFormat.PDF, FormatSniffer.sniff(file))
    }

    @Test
    fun mobiDetectedByMagicAtOffset60() {
        val bytes = ByteArray(512)
        "BOOKMOBI".toByteArray(StandardCharsets.US_ASCII).copyInto(bytes, 60)
        val file = File(folder.root, "classic.mobi")
        file.writeBytes(bytes)
        assertEquals(BookFormat.MOBI, FormatSniffer.sniff(file))
    }

    @Test
    fun txtDetectedByExtensionWhenNoMagicMatches() {
        val file = File(folder.root, "novel.txt")
        file.writeText("三体 · 正文节选")
        assertEquals(BookFormat.TXT, FormatSniffer.sniff(file))
    }

    @Test
    fun unknownWhenNeitherMagicNorExtensionMatches() {
        val file = File(folder.root, "mystery.bin")
        file.writeBytes(ByteArray(256) { (it % 251).toByte() })
        assertEquals(BookFormat.UNKNOWN, FormatSniffer.sniff(file))
    }

    @Test
    fun emptyFileIsUnknown() {
        val file = File(folder.root, "empty")
        file.createNewFile()
        assertEquals(BookFormat.UNKNOWN, FormatSniffer.sniff(file))
    }
}
