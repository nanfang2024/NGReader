package com.book.ng.domain.locator

import com.book.ng.domain.model.ReadMode
import com.book.ng.domain.model.ReadingLocator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LocatorCodecTest {
    private fun locator(mode: ReadMode = ReadMode.PAGING) = ReadingLocator(
        bookId = 42L,
        chapterIndex = 3,
        page = 17,
        scrollOffset = 480,
        mode = mode,
    )

    @Test
    fun encodeProducesCompactV1Format() {
        assertEquals("v1:3:17:480:p", LocatorCodec.encode(locator(ReadMode.PAGING)))
        assertEquals("v1:3:17:480:s", LocatorCodec.encode(locator(ReadMode.SCROLL)))
    }

    @Test
    fun roundTripPreservesPositionFields() {
        val original = locator(ReadMode.SCROLL)
        val decoded = LocatorCodec.decode(LocatorCodec.encode(original))
        assertEquals(original.copy(bookId = 0L), decoded)
    }

    @Test
    fun roundTripHandlesZeroPositions() {
        val origin = ReadingLocator(bookId = 0L, chapterIndex = 0, page = 0, scrollOffset = 0, mode = ReadMode.PAGING)
        assertEquals(origin, LocatorCodec.decode(LocatorCodec.encode(origin)))
    }

    @Test
    fun rejectsUnknownVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            LocatorCodec.decode("v2:3:17:480:p")
        }
    }

    @Test
    fun rejectsWrongSegmentCount() {
        assertThrows(IllegalArgumentException::class.java) {
            LocatorCodec.decode("v1:3:17:p")
        }
    }

    @Test
    fun rejectsNonNumericPositions() {
        assertThrows(IllegalArgumentException::class.java) {
            LocatorCodec.decode("v1:x:17:480:p")
        }
    }

    @Test
    fun rejectsNegativePositions() {
        assertThrows(IllegalArgumentException::class.java) {
            LocatorCodec.decode("v1:-3:17:480:p")
        }
    }

    @Test
    fun rejectsUnknownModeShort() {
        assertThrows(IllegalArgumentException::class.java) {
            LocatorCodec.decode("v1:3:17:480:z")
        }
    }
}
