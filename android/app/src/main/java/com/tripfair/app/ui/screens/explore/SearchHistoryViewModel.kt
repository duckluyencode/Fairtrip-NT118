package com.tripfair.app.ui.screens.explore

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.places.HistoryEntryDto
import com.tripfair.app.data.places.PlacesRepository
import kotlinx.coroutines.launch

data class SearchHistoryUiState(
    val entries: List<HistoryEntryDto> = emptyList(),
    val loading: Boolean = true,
    @param:StringRes val error: Int? = null,
)

/** The user's last searches (kept on the server, 50 at most). Removing is shown at once. */
class SearchHistoryViewModel(private val places: PlacesRepository) : ViewModel() {
    var state by mutableStateOf(SearchHistoryUiState())
        private set

    init {
        load()
    }

    fun load() {
        state = state.copy(loading = true, error = null)
        viewModelScope.launch {
            state = when (val result = places.history()) {
                is ApiResult.Success -> SearchHistoryUiState(entries = result.value, loading = false)
                is ApiResult.Failure -> state.copy(loading = false, error = loadErrorMessage(result.error))
            }
        }
    }

    fun delete(id: Int) {
        val before = state.entries
        state = state.copy(entries = before.filterNot { it.id == id })
        viewModelScope.launch {
            if (places.deleteHistoryEntry(id) is ApiResult.Failure) state = state.copy(entries = before)
        }
    }

    fun clear() {
        val before = state.entries
        state = state.copy(entries = emptyList())
        viewModelScope.launch {
            if (places.clearHistory() is ApiResult.Failure) state = state.copy(entries = before)
        }
    }
}
