package com.app.tmarita.ui.map

import com.app.tmarita.model.PeruRegion

data class PeruMapUiState(
    val isLoading: Boolean = true,
    val viewportWidth: Float = 542.767f,
    val viewportHeight: Float = 792f,
    val regions: List<PeruRegion> = emptyList(),
    val visitedIds: Set<String> = emptySet(),
    val selectedRegion: PeruRegion? = null
) {
    val visitedCount: Int get() = visitedIds.size
    val totalCount: Int get() = regions.size
    val progressPercent: Int
        get() = if (totalCount == 0) 0 else (visitedCount * 100) / totalCount
}
