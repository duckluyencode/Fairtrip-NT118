package com.tripfair.app.ui.screens.group

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.trips.TripSummaryDto
import com.tripfair.app.data.trips.TripsRepository
import com.tripfair.app.ui.screens.trip.tripErrorMessage
import kotlinx.coroutines.launch

/**
 * The user's solo and group trips. [load] runs each time a screen showing them opens, so
 * trips created or joined meanwhile appear; the list on screen stays while it reloads.
 */
class TripsViewModel(private val trips: TripsRepository) : ViewModel() {
    var items by mutableStateOf<List<TripSummaryDto>>(emptyList())
        private set

    /** True until the first answer, so the empty state is not shown before it. */
    var loading by mutableStateOf(true)
        private set

    @get:StringRes
    var error by mutableStateOf<Int?>(null)
        private set

    fun load() {
        viewModelScope.launch {
            if (items.isEmpty()) loading = true
            when (val result = trips.list()) {
                is ApiResult.Success -> {
                    items = result.value
                    error = null
                }
                is ApiResult.Failure -> error = tripErrorMessage(result.error)
            }
            loading = false
        }
    }
}
