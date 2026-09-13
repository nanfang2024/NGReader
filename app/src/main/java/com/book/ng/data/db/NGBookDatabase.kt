package com.book.ng.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BookEntity::class, ProgressEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NGBookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    abstract fun progressDao(): ProgressDao
}
