package com.book.ng.domain.text

import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Test

class TxtCharsetDetectorTest {
    @Test
    fun gbkChineseSampleDetected() {
        val bytes = "中国文字样章，三体问题精选片段，失去人性失去很多。".toByteArray(charset("GBK"))
        assertEquals(TxtCharsetDetector.GBK, TxtCharsetDetector.detect(bytes))
    }

    @Test
    fun utf8WithoutBomDetected() {
        val bytes = "中国文字样章：失去人性，失去很多；失去兽性，失去一切。".toByteArray(StandardCharsets.UTF_8)
        assertEquals(StandardCharsets.UTF_8, TxtCharsetDetector.detect(bytes))
    }

    @Test
    fun utf8WithBomDetected() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val bytes = bom + "中国文字样章".toByteArray(StandardCharsets.UTF_8)
        assertEquals(StandardCharsets.UTF_8, TxtCharsetDetector.detect(bytes))
    }

    @Test
    fun utf16LeWithBomDetected() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) +
            "中国文字样章".toByteArray(StandardCharsets.UTF_16LE)
        assertEquals(StandardCharsets.UTF_16LE, TxtCharsetDetector.detect(bytes))
    }

    @Test
    fun utf16BeWithBomDetected() {
        val bytes = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) +
            "中国文字样章".toByteArray(StandardCharsets.UTF_16BE)
        assertEquals(StandardCharsets.UTF_16BE, TxtCharsetDetector.detect(bytes))
    }

    @Test
    fun pureAsciiDetectedAsUtf8() {
        val bytes = "Chapter 1: The Dawn of a new era, plain ascii only.".toByteArray(StandardCharsets.US_ASCII)
        assertEquals(StandardCharsets.UTF_8, TxtCharsetDetector.detect(bytes))
    }

    @Test
    fun undecodableBytesFallBackToUtf8() {
        val bytes = byteArrayOf(0xC3.toByte(), 0x28)
        assertEquals(StandardCharsets.UTF_8, TxtCharsetDetector.detect(bytes))
    }

    @Test
    fun decodeReplacesInvalidBytesInsteadOfThrowing() {
        val text = TxtCharsetDetector.decode(byteArrayOf(0xC3.toByte(), 0x28), StandardCharsets.UTF_8)
        assertEquals("\uFFFD(", text)
    }

    @Test
    fun emptyBytesDetectedAsUtf8() {
        assertEquals(StandardCharsets.UTF_8, TxtCharsetDetector.detect(ByteArray(0)))
    }
}
