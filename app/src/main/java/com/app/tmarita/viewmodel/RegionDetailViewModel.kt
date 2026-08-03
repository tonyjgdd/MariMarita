package com.app.tmarita.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.tmarita.data.PeruMapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegionDetailUiState(
    val regionId: String = "",
    val visited: Boolean = false,
    val place: String? = null,
    val visitDateMillis: Long? = null,
    val driveLink: String? = null,
    val notes: String? = null
)

@HiltViewModel
class RegionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PeruMapRepository
) : ViewModel() {

    private val regionId: String = checkNotNull(savedStateHandle["regionId"])

    val uiState: StateFlow<RegionDetailUiState> = repository.observeVisit(regionId)
        .map { visit ->
            RegionDetailUiState(
                regionId = regionId,
                visited = visit?.visited ?: false,
                place = visit?.place,
                visitDateMillis = visit?.visitedAt,
                driveLink = visit?.driveLink,
                notes = visit?.note
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RegionDetailUiState(regionId = regionId))

    fun save(visited: Boolean, place: String?, driveLink: String?, notes: String?, visitDateMillis: Long?) {
        viewModelScope.launch {
            repository.saveVisitDetails(
                regionId = regionId,
                visited = visited,
                place = place?.ifBlank { null },
                visitDateMillis = visitDateMillis,
                driveLink = driveLink?.ifBlank { null },
                note = notes?.ifBlank { null }
            )
        }
    }
}