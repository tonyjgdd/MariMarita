package com.app.tmarita.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips WHERE regionId = :regionId ORDER BY visitDateMillis DESC, id DESC")
    fun observeByRegionId(regionId: String): Flow<List<TripEntity>>

    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteById(tripId: Long)

    @Query("SELECT COUNT(*) FROM trips WHERE regionId = :regionId")
    suspend fun countByRegionId(regionId: String): Int
}