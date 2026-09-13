package com.book.ng.data.repository

import com.book.ng.data.db.ProgressDao
import com.book.ng.data.db.ProgressEntity
import com.book.ng.domain.locator.LocatorCodec
import com.book.ng.domain.model.ReadingLocator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ProgressRepository @Inject constructor(
    private val progressDao: ProgressDao,
) {
    suspend fun save(locator: ReadingLocator) {
        progressDao.upsert(
            ProgressEntity(
                bookId = locator.bookId,
                locator = LocatorCodec.encode(locator),
                mode = locator.mode.name,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    fun get(bookId: Long): Flow<ReadingLocator?> =
        progressDao.getByBookId(bookId).map { entity ->
            entity?.let { LocatorCodec.decode(it.locator).copy(bookId = it.bookId) }
        }

    suspend fun delete(bookId: Long) {
        progressDao.deleteByBookId(bookId)
    }
}
