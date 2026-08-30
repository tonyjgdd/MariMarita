package com.app.tmarita.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.tmarita.data.PeruMapRepository
import com.app.tmarita.model.Trip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PeruMapRepository
) : ViewModel() {

    val regionId: String = checkNotNull(savedStateHandle["regionId"])

    val trips: StateFlow<List<Trip>> = repository.observeTrips(regionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTrip(
        place: String?,
        visitDateMillis: Long?,
        driveLink: String?,
        notes: String?,
        photoPath: String?   // 👈 nuevo
    ) {
        viewModelScope.launch {
            repository.addTrip(
                regionId = regionId,
                place = place?.ifBlank { null },
                visitDateMillis = visitDateMillis,
                driveLink = driveLink?.ifBlank { null },
                notes = notes?.ifBlank { null },
                photoPath = photoPath   // 👈 nuevo
            )
        }
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch { repository.deleteTrip(tripId, regionId) }
    }
}