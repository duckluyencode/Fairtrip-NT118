package com.tripfair.app.ui.screens.profile

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.places.PlaceSummaryDto
import com.tripfair.app.data.places.PlacesRepository
import com.tripfair.app.ui.screens.explore.loadErrorMessage
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val items: List<PlaceSummaryDto> = emptyList(),
    /** FOOD, FUN, SCENIC or null for all; filtered on the phone, the list is short. */
    val category: String? = null,
    val loading: Boolean = true,
    @param:StringRes val error: Int? = null,
) {
    val shown: List<PlaceSummaryDto>
        get() = items.filter { category == null || category in it.categories || it.category == category }
}

class FavoritesViewModel(private val places: PlacesRepository) : ViewModel() {
    var state by mutableStateOf(FavoritesUiState())
        private set

    init {
        load()
    }

    fun load() {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            state = when (val result = places.favorites()) {
                is ApiResult.Success -> state.copy(items = result.value, loading = false)
                is ApiResult.Failure -> state.copy(loading = false, error = loadErrorMessage(result.error))
            }
        }
    }

    fun onCategoryChange(category: String?) {
        state = state.copy(category = category)
    }

    /** Removed at once; put back if the backend refuses. */
    fun unsave(id: Int) {
        val before = state.items
        state = state.copy(items = before.filterNot { it.id == id })
        viewModelScope.launch {
            if (places.setFavorite(id, false) is ApiResult.Failure) state = state.copy(items = before)
        }
    }
}
