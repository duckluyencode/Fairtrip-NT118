package com.tripfair.app.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.places.PlaceSummaryDto
import com.tripfair.app.data.places.PlacesRepository
import kotlinx.coroutines.launch

/** Places to suggest on the home screen; empty (and the section hidden) when there are none. */
class HomeViewModel(private val places: PlacesRepository) : ViewModel() {
    var suggestions by mutableStateOf<List<PlaceSummaryDto>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            // Browsing without text: the catalog in development; Google needs the position.
            val result = places.search(query = null, category = null)
            if (result is ApiResult.Success) suggestions = result.value.items
        }
    }
}
