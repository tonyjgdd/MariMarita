package com.app.tmarita.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [VisitedPlaceEntity::class, TripEntity::class],
    version = 3,   // 👈 subido de 2 a 3
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun visitedPlaceDao(): VisitedPlaceDao
    abstract fun tripDao(): TripDao

    companion object {
        const val DB_NAME = "tmarita.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trips (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        regionId TEXT NOT NULL,
                        place TEXT,
                        visitDateMillis INTEGER,
                        driveLink TEXT,
                        notes TEXT
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO trips (regionId, place, visitDateMillis, driveLink, notes)
                    SELECT regionId, place, visitedAt, driveLink, note
                    FROM visited_places
                    WHERE visited = 1
                      AND (place IS NOT NULL OR driveLink IS NOT NULL OR note IS NOT NULL OR visitedAt IS NOT NULL)
                    """.trimIndent()
                )
            }
        }

        // 👇 nueva migración
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN photoPath TEXT")
            }
        }
    }
}