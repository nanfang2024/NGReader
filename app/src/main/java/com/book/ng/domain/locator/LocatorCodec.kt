package com.book.ng.domain.locator

import com.book.ng.domain.model.ReadMode
import com.book.ng.domain.model.ReadingLocator

/**
 * Compact on-disk locator format: `v1:<chapter>:<page>:<offset>:<modeShort>`.
 * bookId is intentionally excluded — progress rows store it as their own column.
 * Decoded locators therefore carry bookId = 0; callers re-associate the id.
 */
object LocatorCodec {
    const val VERSION_PREFIX = "v1"

    private const val MODE_PAGING = "p"
    private const val MODE_SCROLL = "s"

    fun encode(locator: ReadingLocator): String {
        val mode = when (locator.mode) {
            ReadMode.PAGING -> MODE_PAGING
            ReadMode.SCROLL -> MODE_SCROLL
        }
        return "$VERSION_PREFIX:${locator.chapterIndex}:${locator.page}:${locator.scrollOffset}:$mode"
    }

    fun decode(encoded: String): ReadingLocator {
        val parts = encoded.split(":")
        require(parts.size == 5 && parts[0] == VERSION_PREFIX) {
            "Invalid locator version/segments: '$encoded'"
        }
        val chapter = parts[1].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid chapter in locator: '$encoded'")
        val page = parts[2].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid page in locator: '$encoded'")
        val offset = parts[3].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid offset in locator: '$encoded'")
        require(chapter >= 0 && page >= 0 && offset >= 0) {
            "Negative position in locator: '$encoded'"
        }
        val mode = when (parts[4]) {
            MODE_PAGING -> ReadMode.PAGING
            MODE_SCROLL -> ReadMode.SCROLL
            else -> throw IllegalArgumentException("Invalid mode in locator: '$encoded'")
        }
        return ReadingLocator(
            bookId = 0L,
            chapterIndex = chapter,
            page = page,
            scrollOffset = offset,
            mode = mode,
        )
    }
}
