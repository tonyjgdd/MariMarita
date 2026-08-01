package com.app.tmarita.data

import com.app.tmarita.model.PeruRegion
import com.app.tmarita.model.VisitedPlace
import kotlinx.coroutines.flow.Flow

data class PeruMapSnapshot(
    val viewportWidth: Float,
    val viewportHeight: Float,
    val regions: List<PeruRegion>,
    val visited: List<VisitedPlace>
)

interface PeruMapRepository {
    /** Emite geometría + estado de visita cada vez que algo cambia en la BD. */
    fun observeMap(): Flow<PeruMapSnapshot>

    suspend fun markVisited(regionId: String, note: String? = null, photoUri: String? = null)
    suspend fun unmark(regionId: String)
}
