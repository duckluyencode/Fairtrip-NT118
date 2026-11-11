package com.tripfair.app.ui.screens.explore

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.places.PageCursor
import com.tripfair.app.data.places.PlaceSummaryDto
import com.tripfair.app.data.places.PlacesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class SearchResultsUiState(
    val query: String = "",
    /** FOOD, FUN, SCENIC or null for all. */
    val category: String? = null,
    val items: List<PlaceSummaryDto> = emptyList(),
    val loading: Boolean = false,
    @param:StringRes val error: Int? = null,
    val next: PageCursor? = null,
    val loadingMore: Boolean = false,
)

/** Results of one query, filtered by category, one page at a time. */
class SearchResultsViewModel(private val places: PlacesRepository) : ViewModel() {
    var state by mutableStateOf(SearchResultsUiState())
        private set

    private var started = false
    private var loading: Job? = null

    /** Called by the screen with its query; later calls change nothing. */
    fun start(query: String) {
        if (started) return
        started = true
        state = state.copy(query = query)
        reload()
    }

    fun onCategoryChange(category: String?) {
        if (category == state.category) return
        state = state.copy(category = category)
        reload()
    }

    fun reload() {
        loading?.cancel()
        state = state.copy(items = emptyList(), next = null, loading = true, error = null, loadingMore = false)
        loading = viewModelScope.launch {
            state = when (val result = places.search(state.query, state.category)) {
                is ApiResult.Success -> state.copy(loading = false, items = result.value.items, next = result.value.next)
                is ApiResult.Failure -> state.copy(loading = false, error = loadErrorMessage(result.error))
            }
        }
    }

    fun loadMore() {
        val next = state.next ?: return
        if (state.loadingMore || state.loading) return
        state = state.copy(loadingMore = true)
        loading = viewModelScope.launch {
            state = when (val result = places.search(state.query, state.category, next)) {
                is ApiResult.Success -> state.copy(
                    loadingMore = false,
                    items = (state.items + result.value.items).distinctBy { it.id },
                    next = result.value.next,
                )
                // Keep what is shown; the "Xem thêm" button stays for another try.
                is ApiResult.Failure -> state.copy(loadingMore = false)
            }
        }
    }

    /** Shows the change at once and undoes it if the backend refuses. */
    fun toggleFavorite(id: Int) {
        val item = state.items.firstOrNull { it.id == id } ?: return
        val favorite = !item.isFavorite
        setFavorite(id, favorite)
        viewModelScope.launch {
            if (places.setFavorite(id, favorite) is ApiResult.Failure) setFavorite(id, !favorite)
        }
    }

    private fun setFavorite(id: Int, favorite: Boolean) {
        state = state.copy(items = state.items.map { if (it.id == id) it.copy(isFavorite = favorite) else it })
    }
}
