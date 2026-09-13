package com.book.ng.domain.text

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object TxtCharsetDetector {
    val GBK: Charset = Charset.forName("GBK")
    private const val CJK_RATIO_THRESHOLD = 0.6
    private val UTF_8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private val UTF_16_LE_BOM = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
    private val UTF_16_BE_BOM = byteArrayOf(0xFE.toByte(), 0xFF.toByte())

    fun detect(bytes: ByteArray): Charset {
        if (startsWith(bytes, UTF_8_BOM)) return StandardCharsets.UTF_8
        if (startsWith(bytes, UTF_16_LE_BOM)) return StandardCharsets.UTF_16LE
        if (startsWith(bytes, UTF_16_BE_BOM)) return StandardCharsets.UTF_16BE
        if (strictDecode(StandardCharsets.UTF_8.newDecoder(), bytes) != null) {
            return StandardCharsets.UTF_8
        }
        val gbkText = strictDecode(GBK.newDecoder(), bytes)
        if (gbkText != null && cjkRatio(gbkText) >= CJK_RATIO_THRESHOLD) {
            return GBK
        }
        return StandardCharsets.UTF_8
    }

    fun decode(bytes: ByteArray, charset: Charset): String {
        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
        return decoder.decode(ByteBuffer.wrap(bytes)).toString()
    }

    fun stripBom(text: String): String =
        if (text.isNotEmpty() && text[0] == '\uFEFF') text.substring(1) else text

    private fun strictDecode(decoder: CharsetDecoder, bytes: ByteArray): String? {
        decoder.onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: CharacterCodingException) {
            null
        }
    }

    private fun cjkRatio(text: String): Double {
        val meaningful = text.filterNot { it.isWhitespace() }
        if (meaningful.isEmpty()) return 0.0
        val cjk = meaningful.count { it.code in 0x4E00..0x9FFF }
        return cjk.toDouble() / meaningful.length
    }

    private fun startsWith(bytes: ByteArray, prefix: ByteArray): Boolean {
        if (bytes.size < prefix.size) return false
        return prefix.indices.all { bytes[it] == prefix[it] }
    }
}
