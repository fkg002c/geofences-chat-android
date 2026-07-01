package com.ruinkogr.chatapp.di

import android.content.Context
import androidx.room.Room
import com.ruinkogr.chatapp.data.local.AppDatabase
import com.ruinkogr.chatapp.data.local.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "chat_app_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }
}