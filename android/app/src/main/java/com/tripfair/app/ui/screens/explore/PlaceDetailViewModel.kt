package com.tripfair.app.ui.screens.explore

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.R
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.places.PlaceDetailDto
import com.tripfair.app.data.places.PlacesRepository
import kotlinx.coroutines.launch

data class PlaceDetailUiState(
    val place: PlaceDetailDto? = null,
    val loading: Boolean = true,
    @param:StringRes val error: Int? = null,
)

/** One place with its price, hours and visit time (GET /places/:id). */
class PlaceDetailViewModel(private val places: PlacesRepository) : ViewModel() {
    var state by mutableStateOf(PlaceDetailUiState())
        private set

    private var id: Int? = null
    private var sessionToken: String? = null

    /** [placeId] comes from the route; ids that are not numbers belong to sample screens. */
    fun start(placeId: String, sessionToken: String?) {
        if (id != null || state.error != null) return
        val parsed = placeId.toIntOrNull()
        if (parsed == null) {
            state = PlaceDetailUiState(loading = false, error = R.string.error_place_not_found)
            return
        }
        id = parsed
        this.sessionToken = sessionToken
        load()
    }

    fun retry() {
        if (id != null) load()
    }

    private fun load() {
        val placeId = id ?: return
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            state = when (val result = places.detail(placeId, sessionToken)) {
                is ApiResult.Success -> PlaceDetailUiState(place = result.value, loading = false)
                is ApiResult.Failure -> state.copy(loading = false, error = loadErrorMessage(result.error))
            }
            // The Autocomplete session ends when a place is opened.
            sessionToken = null
        }
    }

    fun toggleFavorite() {
        val place = state.place ?: return
        val favorite = !place.isFavorite
        state = state.copy(place = place.copy(isFavorite = favorite))
        viewModelScope.launch {
            if (places.setFavorite(place.id, favorite) is ApiResult.Failure) {
                state = state.copy(place = state.place?.copy(isFavorite = !favorite))
            }
        }
    }
}
