package com.book.ng.di

import android.content.Context
import androidx.room.Room
import com.book.ng.data.db.BookDao
import com.book.ng.data.db.NGBookDatabase
import com.book.ng.data.db.ProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NGBookDatabase =
        Room.databaseBuilder(context, NGBookDatabase::class.java, "ngbook.db").build()

    @Provides
    fun provideBookDao(database: NGBookDatabase): BookDao = database.bookDao()

    @Provides
    fun provideProgressDao(database: NGBookDatabase): ProgressDao = database.progressDao()
}
