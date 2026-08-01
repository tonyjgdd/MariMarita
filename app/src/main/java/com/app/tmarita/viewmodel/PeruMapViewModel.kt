package com.app.tmarita.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.tmarita.data.PeruMapRepository
import com.app.tmarita.data.source.PeruRegionsAssetSource
import com.app.tmarita.model.PeruRegion
import com.app.tmarita.ui.map.PeruMapUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PeruMapViewModel @Inject constructor(
    private val repository: PeruMapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeruMapUiState())
    val uiState: StateFlow<PeruMapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeMap().collect { snapshot ->
                val visitableRegions = snapshot.regions.filter {
                    it.id !in PeruRegionsAssetSource.NON_VISITABLE_IDS
                }
                // Lima/Callao/Lima Metropolitana están SIEMPRE visitados, sin importar la BD.
                val visitedFromDb = snapshot.visited.map { it.regionId }.toSet()
                val effectiveVisited = visitedFromDb + PeruRegionsAssetSource.LIMA_GROUP_IDS

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    viewportWidth = snapshot.viewportWidth,
                    viewportHeight = snapshot.viewportHeight,
                    regions = visitableRegions,
                    visitedIds = effectiveVisited
                )
            }
        }
    }

    /** Tocar un departamento en el mapa: solo lo selecciona (muestra la tarjeta de info). */
    fun onRegionTapped(region: PeruRegion) {
        _uiState.value = _uiState.value.copy(selectedRegion = region)
    }

    /**
     * El switch de la tarjeta de info: marca o desmarca como visitado.
     * Ignora el intento si es una de las regiones del grupo Lima (siempre visitado, fijo).
     */
    fun setVisited(regionId: String, visited: Boolean) {
        if (regionId in PeruRegionsAssetSource.LIMA_GROUP_IDS) return
        viewModelScope.launch {
            if (visited) repository.markVisited(regionId) else repository.unmark(regionId)
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedRegion = null)
    }
}