package com.tripfair.app.ui.screens.explore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.places.PlacesRepository
import com.tripfair.app.data.places.SuggestionDto
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Wait this long after the last key before asking for suggestions. */
private const val TYPING_PAUSE_MILLIS = 300L
private const val RECENT_COUNT = 5

data class SearchUiState(
    val query: String = "",
    val suggestions: List<SuggestionDto> = emptyList(),
    /** Latest distinct searches from the history. */
    val recent: List<String> = emptyList(),
)

/**
 * Suggestions while typing (Google Place Autocomplete through the backend). One session
 * token per visit: Google then bills the typing together with the place that is opened.
 */
class SearchViewModel(private val places: PlacesRepository) : ViewModel() {
    var state by mutableStateOf(SearchUiState())
        private set

    val sessionToken: String = UUID.randomUUID().toString()

    private var typing: Job? = null

    init {
        viewModelScope.launch {
            val history = places.history()
            if (history is ApiResult.Success) {
                state = state.copy(recent = history.value.map { it.query }.distinct().take(RECENT_COUNT))
            }
        }
    }

    fun onQueryChange(value: String) {
        state = state.copy(query = value)
        typing?.cancel()
        if (value.isBlank()) {
            state = state.copy(suggestions = emptyList())
            return
        }
        typing = viewModelScope.launch {
            delay(TYPING_PAUSE_MILLIS)
            // A failed suggestion request is not worth an error: the user can still search.
            val result = places.suggestions(value.trim(), sessionToken)
            if (result is ApiResult.Success) state = state.copy(suggestions = result.value)
        }
    }
}
