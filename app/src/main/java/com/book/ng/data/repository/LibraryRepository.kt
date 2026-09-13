package com.book.ng.data.repository

import android.content.Context
import com.book.ng.data.db.BookDao
import com.book.ng.data.db.BookEntity
import com.book.ng.data.db.toDomain
import com.book.ng.domain.model.BookFormat
import com.book.ng.domain.model.LibraryBook
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class LibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
) {
    val books: Flow<List<LibraryBook>> = bookDao.getAll().map { rows -> rows.map(BookEntity::toDomain) }

    suspend fun importFile(displayName: String, source: InputStream): LibraryBook {
        val extension = displayName.substringAfterLast('.', "bin").lowercase()
        val storedName = "${UUID.randomUUID()}.$extension"
        val target = File(booksDir(), storedName)
        withContext(Dispatchers.IO) {
            target.outputStream().use { output -> source.copyTo(output) }
        }
        val entity = BookEntity(
            title = displayName.substringBeforeLast('.'),
            author = null,
            fileName = storedName,
            format = extensionToFormat(extension).name,
            addedAt = System.currentTimeMillis(),
            lastReadAt = null,
        )
        val id = bookDao.upsert(entity)
        return entity.copy(id = id).toDomain()
    }

    suspend fun delete(bookId: Long) {
        val entity = bookDao.getById(bookId) ?: return
        bookDao.deleteById(bookId)
        withContext(Dispatchers.IO) {
            File(booksDir(), entity.fileName).delete()
        }
    }

    fun fileFor(book: LibraryBook): File = File(booksDir(), book.fileName)

    private fun booksDir(): File = File(context.filesDir, "books").apply { mkdirs() }

    private fun extensionToFormat(extension: String): BookFormat = when (extension) {
        "epub" -> BookFormat.EPUB
        "txt" -> BookFormat.TXT
        "mobi" -> BookFormat.MOBI
        "azw3" -> BookFormat.AZW3
        "pdf" -> BookFormat.PDF
        "cbz" -> BookFormat.CBZ
        "zip" -> BookFormat.ZIP
        else -> BookFormat.UNKNOWN
    }
}
