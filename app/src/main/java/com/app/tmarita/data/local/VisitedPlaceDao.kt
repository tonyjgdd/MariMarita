package com.app.tmarita.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitedPlaceDao {

    @Query("SELECT * FROM visited_places WHERE visited = 1")
    fun observeAll(): Flow<List<VisitedPlaceEntity>>

    @Query("SELECT * FROM visited_places WHERE regionId = :regionId")
    fun observeByRegionId(regionId: String): Flow<VisitedPlaceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(place: VisitedPlaceEntity)

    @Query("DELETE FROM visited_places WHERE regionId = :regionId")
    suspend fun deleteByRegionId(regionId: String)

    @Query("SELECT COUNT(*) FROM visited_places WHERE visited = 1")
    suspend fun countVisited(): Int
}