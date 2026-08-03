package com.app.tmarita.di

import android.content.Context
import androidx.room.Room
import com.app.tmarita.data.PeruMapRepository
import com.app.tmarita.data.PeruMapRepositoryImpl
import com.app.tmarita.data.local.AppDatabase
import com.app.tmarita.data.local.VisitedPlaceDao
import dagger.Binds
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DB_NAME).build()

    @Provides
    fun provideVisitedPlaceDao(db: AppDatabase): VisitedPlaceDao = db.visitedPlaceDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPeruMapRepository(impl: PeruMapRepositoryImpl): PeruMapRepository
}