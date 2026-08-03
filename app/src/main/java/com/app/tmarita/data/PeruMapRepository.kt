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
    fun observeMap(): Flow<PeruMapSnapshot>

    /** Para la pantalla de detalle: todo lo guardado de un departamento (o null si nunca se guardó nada). */
    fun observeVisit(regionId: String): Flow<VisitedPlace?>

    suspend fun markVisited(regionId: String, note: String? = null, photoUri: String? = null)
    suspend fun unmark(regionId: String)

    /** Guarda el formulario completo del detalle de un departamento. */
    suspend fun saveVisitDetails(
        regionId: String,
        visited: Boolean,
        place: String?,
        visitDateMillis: Long?,
        driveLink: String?,
        note: String?
    )
}