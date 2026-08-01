package com.app.tmarita.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [VisitedPlaceEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun visitedPlaceDao(): VisitedPlaceDao

    companion object {
        const val DB_NAME = "tmarita.db"
    }
}
