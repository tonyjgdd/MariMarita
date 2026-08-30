package com.app.tmarita.data

import com.app.tmarita.data.local.TripDao
import com.app.tmarita.data.local.TripEntity
import com.app.tmarita.data.local.VisitedPlaceDao
import com.app.tmarita.data.local.VisitedPlaceEntity
import com.app.tmarita.data.source.PeruRegionsAssetSource
import com.app.tmarita.model.Trip
import com.app.tmarita.model.VisitedPlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeruMapRepositoryImpl @Inject constructor(
    private val assetSource: PeruRegionsAssetSource,
    private val dao: VisitedPlaceDao,
    private val tripDao: TripDao
) : PeruMapRepository {

    override fun observeMap(): Flow<PeruMapSnapshot> {
        val geometry = assetSource.load()
        return dao.observeAll().map { entities ->
            PeruMapSnapshot(
                viewportWidth = geometry.viewportWidth,
                viewportHeight = geometry.viewportHeight,
                regions = geometry.regions,
                visited = entities.map { it.toDomain() }
            )
        }
    }

    override fun observeVisit(regionId: String): Flow<VisitedPlace?> =
        dao.observeByRegionId(regionId).map { it?.toDomain() }

    override suspend fun markVisited(regionId: String, note: String?, photoUri: String?) {
        dao.upsert(
            VisitedPlaceEntity(
                regionId = regionId,
                visited = true,
                visitedAt = System.currentTimeMillis(),
                note = note,
                photoUri = photoUri
            )
        )
    }

    override suspend fun unmark(regionId: String) {
        dao.deleteByRegionId(regionId)
    }

    override suspend fun saveVisitDetails(
        regionId: String,
        visited: Boolean,
        place: String?,
        visitDateMillis: Long?,
        driveLink: String?,
        note: String?
    ) {
        dao.upsert(
            VisitedPlaceEntity(
                regionId = regionId,
                visited = visited,
                visitedAt = visitDateMillis,
                place = place,
                driveLink = driveLink,
                note = note
            )
        )
    }

    // ---------- Viajes ----------

    override fun observeTrips(regionId: String): Flow<List<Trip>> =
        tripDao.observeByRegionId(regionId).map { list -> list.map { it.toDomain() } }

    override suspend fun addTrip(
        regionId: String,
        place: String?,
        visitDateMillis: Long?,
        driveLink: String?,
        notes: String?,
        photoPath: String?   // 👈 nuevo
    ) {
        tripDao.insert(
            TripEntity(
                regionId = regionId,
                place = place,
                visitDateMillis = visitDateMillis,
                driveLink = driveLink,
                notes = notes,
                photoPath = photoPath   // 👈 nuevo
            )
        )
        // Al agregar un viaje, el departamento queda visitado automáticamente.
        dao.upsert(
            VisitedPlaceEntity(
                regionId = regionId,
                visited = true,
                visitedAt = visitDateMillis ?: System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteTrip(tripId: Long, regionId: String) {
        tripDao.deleteById(tripId)
        if (tripDao.countByRegionId(regionId) == 0) {
            dao.deleteByRegionId(regionId)
        }
    }

    private fun VisitedPlaceEntity.toDomain() = VisitedPlace(
        regionId = regionId,
        visited = visited,
        visitedAt = visitedAt,
        place = place,
        driveLink = driveLink,
        note = note,
        photoUri = photoUri
    )

    private fun TripEntity.toDomain() = Trip(
        id = id,
        regionId = regionId,
        place = place,
        visitDateMillis = visitDateMillis,
        driveLink = driveLink,
        notes = notes,
        photoPath = photoPath   // 👈 nuevo
    )
}