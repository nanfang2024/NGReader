package com.book.ng.data.importer

import com.book.ng.domain.model.BookFormat
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

object FormatSniffer {
    private const val WINDOW_BYTES = 64 * 1024
    private const val MOBI_MAGIC_OFFSET = 60
    private val ZIP_LOCAL_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
    private val PDF_MAGIC = "%PDF".toByteArray(StandardCharsets.US_ASCII)
    private val MOBI_MAGIC = "BOOKMOBI".toByteArray(StandardCharsets.US_ASCII)
    private val CONTAINER_XML = "META-INF/container.xml"
    private val IMAGE_SUFFIXES = listOf(
        ".jpg",
        ".jpeg",
        ".png",
        ".webp",
        ".gif",
        ".bmp",
        ".avif",
        ".tiff",
    )

    fun sniff(file: File): BookFormat {
        val head = readHead(file)
        val tail = if (file.length() > head.size) readTail(file) else head
        return sniff(head, tail, file.name)
    }

    fun sniff(head: ByteArray, tail: ByteArray, fileName: String): BookFormat {
        if (startsWith(head, PDF_MAGIC)) return BookFormat.PDF
        if (startsWith(head, ZIP_LOCAL_MAGIC)) {
            val entryText = String(head, StandardCharsets.ISO_8859_1) +
                String(tail, StandardCharsets.ISO_8859_1)
            if (entryText.contains(CONTAINER_XML)) return BookFormat.EPUB
            if (IMAGE_SUFFIXES.any { entryText.contains(it, ignoreCase = true) }) {
                return BookFormat.CBZ
            }
            return BookFormat.ZIP
        }
        if (head.size >= MOBI_MAGIC_OFFSET + MOBI_MAGIC.size &&
            head.copyOfRange(MOBI_MAGIC_OFFSET, MOBI_MAGIC_OFFSET + MOBI_MAGIC.size)
                    .contentEquals(MOBI_MAGIC)
        ) {
            return BookFormat.MOBI
        }
        if (fileName.substringAfterLast('.', "").equals("txt", ignoreCase = true)) {
            return BookFormat.TXT
        }
        return BookFormat.UNKNOWN
    }

    private fun startsWith(bytes: ByteArray, prefix: ByteArray): Boolean {
        if (bytes.size < prefix.size) return false
        return prefix.indices.all { bytes[it] == prefix[it] }
    }

    private fun readHead(file: File): ByteArray {
        val len = minOf(file.length(), WINDOW_BYTES.toLong()).toInt()
        val buffer = ByteArray(len)
        if (len == 0) return buffer
        RandomAccessFile(file, "r").use { it.readFully(buffer) }
        return buffer
    }

    private fun readTail(file: File): ByteArray {
        val len = minOf(file.length(), WINDOW_BYTES.toLong()).toInt()
        val buffer = ByteArray(len)
        if (len == 0) return buffer
        RandomAccessFile(file, "r").use {
            it.seek(file.length() - len)
            it.readFully(buffer)
        }
        return buffer
    }
}
