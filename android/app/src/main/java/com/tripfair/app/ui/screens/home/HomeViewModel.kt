package com.tripfair.app.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.places.PlaceSummaryDto
import com.tripfair.app.data.places.PlacesRepository
import com.tripfair.app.data.trips.TripSummaryDto
import com.tripfair.app.data.trips.TripsRepository
import com.tripfair.app.ui.screens.trip.upcoming
import kotlinx.coroutines.launch

/**
 * Places to suggest on the home screen, and the user's next trip; each section is hidden
 * when there is nothing to show.
 */
class HomeViewModel(private val places: PlacesRepository, private val trips: TripsRepository) : ViewModel() {
    var suggestions by mutableStateOf<List<PlaceSummaryDto>>(emptyList())
        private set

    var upcomingTrip by mutableStateOf<TripSummaryDto?>(null)
        private set

    init {
        viewModelScope.launch {
            // Browsing without text: the catalog in development; Google needs the position.
            val result = places.search(query = null, category = null)
            if (result is ApiResult.Success) suggestions = result.value.items
        }
        viewModelScope.launch {
            val result = trips.list()
            if (result is ApiResult.Success) upcomingTrip = result.value.upcoming()
        }
    }
}
