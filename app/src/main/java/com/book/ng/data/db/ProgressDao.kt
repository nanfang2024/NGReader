package com.book.ng.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Query("SELECT * FROM progress WHERE bookId = :bookId")
    fun getByBookId(bookId: Long): Flow<ProgressEntity?>

    @Query("SELECT COUNT(*) FROM progress")
    suspend fun count(): Int

    @Query("DELETE FROM progress WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: Long)
}
