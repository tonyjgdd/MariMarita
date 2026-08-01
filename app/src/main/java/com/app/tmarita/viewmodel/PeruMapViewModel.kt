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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    viewportWidth = snapshot.viewportWidth,
                    viewportHeight = snapshot.viewportHeight,
                    regions = visitableRegions,
                    visitedIds = snapshot.visited.map { it.regionId }.toSet()
                )
            }
        }
    }

    fun onRegionTapped(region: PeruRegion) {
        viewModelScope.launch {
            if (region.id in _uiState.value.visitedIds) {
                repository.unmark(region.id)
            } else {
                repository.markVisited(region.id)
            }
        }
        _uiState.value = _uiState.value.copy(selectedRegion = region)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedRegion = null)
    }
}
