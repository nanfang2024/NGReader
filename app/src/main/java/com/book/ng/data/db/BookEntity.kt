package com.book.ng.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.book.ng.domain.model.BookFormat
import com.book.ng.domain.model.LibraryBook

@Entity(
    tableName = "books",
    indices = [Index("addedAt")],
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String?,
    val fileName: String,
    val format: String,
    val addedAt: Long,
    val lastReadAt: Long?,
)

fun BookEntity.toDomain(): LibraryBook = LibraryBook(
    id = id,
    title = title,
    author = author,
    fileName = fileName,
    format = runCatching { BookFormat.valueOf(format) }.getOrDefault(BookFormat.UNKNOWN),
    addedAt = addedAt,
    lastReadAt = lastReadAt,
)
