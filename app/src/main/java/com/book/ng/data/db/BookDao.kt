package com.book.ng.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: BookEntity): Long

    @Query(
        """
        SELECT * FROM books
        ORDER BY lastReadAt IS NULL, lastReadAt DESC, addedAt DESC
        """,
    )
    fun getAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): BookEntity?

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: Long)
}
