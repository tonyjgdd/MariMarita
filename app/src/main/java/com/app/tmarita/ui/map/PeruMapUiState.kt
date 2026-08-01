package com.app.tmarita.ui.map

import com.app.tmarita.data.source.PeruRegionsAssetSource
import com.app.tmarita.model.PeruRegion

data class PeruMapUiState(
    val isLoading: Boolean = true,
    val viewportWidth: Float = 542.767f,
    val viewportHeight: Float = 792f,
    val regions: List<PeruRegion> = emptyList(),
    val visitedIds: Set<String> = emptySet(), // ya incluye el grupo Lima, ver ViewModel
    val selectedRegion: PeruRegion? = null
) {
    private val limaGroupIds get() = PeruRegionsAssetSource.LIMA_GROUP_IDS

    /** Departamentos "normales" (todo menos las 3 formas del grupo Lima). */
    private val singleRegions get() = regions.filter { it.id !in limaGroupIds }

    /** 23 seleccionables + 1 grupo Lima fijo = 24 (si el grupo Lima está presente en el JSON). */
    val totalCount: Int
        get() = singleRegions.size + if (regions.any { it.id in limaGroupIds }) 1 else 0

    val visitedCount: Int
        get() = singleRegions.count { it.id in visitedIds } +
                if (regions.any { it.id in limaGroupIds }) 1 else 0

    val progressPercent: Int
        get() = if (totalCount == 0) 0 else (visitedCount * 100) / totalCount

    fun isLimaGroup(id: String): Boolean = id in limaGroupIds
}