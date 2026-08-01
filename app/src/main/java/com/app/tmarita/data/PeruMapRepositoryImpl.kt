package com.app.tmarita.data

import com.app.tmarita.data.local.VisitedPlaceDao
import com.app.tmarita.data.local.VisitedPlaceEntity
import com.app.tmarita.data.source.PeruRegionsAssetSource
import com.app.tmarita.model.VisitedPlace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeruMapRepositoryImpl @Inject constructor(
    private val assetSource: PeruRegionsAssetSource,
    private val dao: VisitedPlaceDao
) : PeruMapRepository {

    override fun observeMap(): Flow<PeruMapSnapshot> {
        val geometry = assetSource.load()
        return dao.observeAll().map { entities ->
            PeruMapSnapshot(
                viewportWidth = geometry.viewportWidth,
                viewportHeight = geometry.viewportHeight,
                regions = geometry.regions,
                visited = entities.map {
                    VisitedPlace(it.regionId, it.visitedAt, it.note, it.photoUri)
                }
            )
        }
    }

    override suspend fun markVisited(regionId: String, note: String?, photoUri: String?) {
        dao.upsert(
            VisitedPlaceEntity(
                regionId = regionId,
                visitedAt = System.currentTimeMillis(),
                note = note,
                photoUri = photoUri
            )
        )
    }

    override suspend fun unmark(regionId: String) {
        dao.deleteByRegionId(regionId)
    }
}
