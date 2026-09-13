package com.book.ng.data.db

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DaoTest {
    private lateinit var database: NGBookDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            NGBookDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertThenFlowEmitsBookThenDeleteEmptiesTable() = runBlocking {
        val dao = database.bookDao()
        dao.upsert(
            BookEntity(
                title = "三体",
                author = "刘慈欣",
                fileName = "santi.txt",
                format = "TXT",
                addedAt = 1_000L,
                lastReadAt = null,
            ),
        )
        val books = dao.getAll().first()
        assertEquals(1, books.size)
        assertEquals("三体", books.first().title)

        dao.deleteById(books.first().id)
        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun getAllOrdersRecentlyReadFirst() = runBlocking {
        val dao = database.bookDao()
        dao.upsert(BookEntity(title = "旧读", author = null, fileName = "a.txt", format = "TXT", addedAt = 1L, lastReadAt = 10L))
        dao.upsert(BookEntity(title = "新读", author = null, fileName = "b.txt", format = "TXT", addedAt = 2L, lastReadAt = 20L))
        dao.upsert(BookEntity(title = "未读", author = null, fileName = "c.txt", format = "TXT", addedAt = 3L, lastReadAt = null))
        val titles = dao.getAll().first().map { it.title }
        assertEquals(listOf("新读", "旧读", "未读"), titles)
    }

    @Test
    fun progressUpsertOverwritesWithoutDuplicates() = runBlocking {
        val bookId = database.bookDao().upsert(
            BookEntity(title = "流浪地球", author = null, fileName = "d.txt", format = "TXT", addedAt = 1L, lastReadAt = null),
        )
        val dao = database.progressDao()
        dao.upsert(ProgressEntity(bookId = bookId, locator = "v1:0:1:0:p", mode = "PAGING", updatedAt = 5L))
        dao.upsert(ProgressEntity(bookId = bookId, locator = "v1:0:2:0:p", mode = "PAGING", updatedAt = 9L))

        val row = dao.getByBookId(bookId).first()
        assertNotNull(row)
        assertEquals("v1:0:2:0:p", row?.locator)
        assertEquals(1, dao.count())
    }

    @Test
    fun deletingBookCascadesProgress() = runBlocking {
        val bookId = database.bookDao().upsert(
            BookEntity(title = "深海", author = null, fileName = "e.txt", format = "TXT", addedAt = 1L, lastReadAt = null),
        )
        database.progressDao().upsert(
            ProgressEntity(bookId = bookId, locator = "v1:1:3:100:s", mode = "SCROLL", updatedAt = 1L),
        )
        assertNotNull(database.progressDao().getByBookId(bookId).first())

        database.bookDao().deleteById(bookId)
        assertNull(database.progressDao().getByBookId(bookId).first())
    }
}
