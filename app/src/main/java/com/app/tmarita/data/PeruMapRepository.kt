package com.app.tmarita.data

import com.app.tmarita.model.PeruRegion
import com.app.tmarita.model.Trip
import com.app.tmarita.model.VisitedPlace
import kotlinx.coroutines.flow.Flow

data class PeruMapSnapshot(
    val viewportWidth: Float,
    val viewportHeight: Float,
    val regions: List<PeruRegion>,
    val visited: List<VisitedPlace>
)

interface PeruMapRepository {
    fun observeMap(): Flow<PeruMapSnapshot>

    fun observeVisit(regionId: String): Flow<VisitedPlace?>

    suspend fun markVisited(regionId: String, note: String? = null, photoUri: String? = null)
    suspend fun unmark(regionId: String)

    suspend fun saveVisitDetails(
        regionId: String,
        visited: Boolean,
        place: String?,
        visitDateMillis: Long?,
        driveLink: String?,
        note: String?
    )

    // 👇 Nuevo: manejo de viajes (varios por departamento)
    fun observeTrips(regionId: String): Flow<List<Trip>>

    suspend fun addTrip(
        regionId: String,
        place: String?,
        visitDateMillis: Long?,
        driveLink: String?,
        notes: String?,
        photoPath: String?   // 👈 nuevo
    )

    suspend fun deleteTrip(tripId: Long, regionId: String)
}