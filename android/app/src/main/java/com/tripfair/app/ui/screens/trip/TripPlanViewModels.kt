package com.tripfair.app.ui.screens.trip

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.data.network.ApiError
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.trips.ItineraryDto
import com.tripfair.app.data.trips.LodgingOptionDto
import com.tripfair.app.data.trips.TripDetailDto
import com.tripfair.app.data.trips.TripsRepository
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** "Tự lo chỗ ở" in [LodgingViewModel.selected]. */
const val SELF_LODGING = -1

/** "Chọn chỗ ở": the three suggested hotels of a trip, or "Tự lo chỗ ở". */
class LodgingViewModel(private val trips: TripsRepository) : ViewModel() {
    var trip by mutableStateOf<TripDetailDto?>(null)
        private set
    var options by mutableStateOf<List<LodgingOptionDto>>(emptyList())
        private set

    /** When the prices were read, "HH:mm". */
    var fetchedAt by mutableStateOf("")
        private set

    /** The chosen hotel's placeId, or [SELF_LODGING]. */
    var selected by mutableIntStateOf(SELF_LODGING)
        private set
    var loading by mutableStateOf(true)
        private set
    var saving by mutableStateOf(false)
        private set

    @get:StringRes
    var error by mutableStateOf<Int?>(null)
        private set
    var done by mutableStateOf(false)
        private set

    private var tripId = 0

    fun start(tripId: Int) {
        if (this.tripId == tripId) return
        this.tripId = tripId
        load()
    }

    fun load() {
        viewModelScope.launch {
            loading = true
            error = null
            val detail = async { trips.detail(tripId) }
            val hotels = trips.lodgingOptions(tripId, guestsPerRoom = null)
            (detail.await() as? ApiResult.Success)?.let { trip = it.value }
            when (hotels) {
                is ApiResult.Success -> {
                    options = hotels.value
                    fetchedAt = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    // The balanced hotel is the recommended one, as in the design.
                    selected = (hotels.value.firstOrNull { it.tag == "BALANCED" } ?: hotels.value.firstOrNull())?.placeId ?: SELF_LODGING
                }
                // A one-day trip has no night to book: only "Tự lo chỗ ở" is offered.
                is ApiResult.Failure -> if (hotels.error.code != "LODGING_NOT_NEEDED") error = tripErrorMessage(hotels.error)
            }
            loading = false
        }
    }

    fun select(placeId: Int) {
        selected = placeId
        error = null
    }

    fun confirm() {
        if (saving) return
        viewModelScope.launch {
            saving = true
            error = null
            val hotel = options.firstOrNull { it.placeId == selected }
            when (val result = trips.chooseLodging(tripId, hotel?.placeId, hotel?.guestsPerRoom)) {
                is ApiResult.Success -> done = true
                is ApiResult.Failure -> error = tripErrorMessage(result.error)
            }
            saving = false
        }
    }

    fun onNavigated() {
        done = false
    }
}

/** Number of steps "Đang lập lộ trình" shows. */
const val PLANNING_STEPS = 4

/**
 * "Đang lập lộ trình": asks the backend to plan the trip. The steps on screen advance on a
 * timer while the request runs and wait at the last one until it answers.
 */
class OptimizingViewModel(private val trips: TripsRepository) : ViewModel() {
    var step by mutableIntStateOf(0)
        private set

    var error by mutableStateOf<ApiError?>(null)
        private set

    var done by mutableStateOf(false)
        private set

    private var tripId = 0

    fun start(tripId: Int) {
        if (this.tripId == tripId) return
        this.tripId = tripId
        plan()
    }

    fun plan() {
        error = null
        step = 0
        viewModelScope.launch {
            val ticker = launch {
                while (step < PLANNING_STEPS - 1) {
                    delay(STEP_MILLIS)
                    step++
                }
            }
            val result = trips.plan(tripId)
            ticker.cancel()
            when (result) {
                is ApiResult.Success -> {
                    step = PLANNING_STEPS
                    delay(STEP_MILLIS / 2)
                    done = true
                }
                is ApiResult.Failure -> error = result.error
            }
        }
    }

    private companion object {
        const val STEP_MILLIS = 600L
    }
}

/** The planned itinerary of a trip, with the trip itself for its title and members. */
class ItineraryViewModel(private val trips: TripsRepository) : ViewModel() {
    var trip by mutableStateOf<TripDetailDto?>(null)
        private set
    var itinerary by mutableStateOf<ItineraryDto?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<ApiError?>(null)
        private set

    private var tripId = 0

    /** Each time the screen shows; what is on screen stays while it reloads. */
    fun start(tripId: Int) {
        this.tripId = tripId
        load()
    }

    fun load() {
        viewModelScope.launch {
            loading = itinerary == null
            error = null
            val detail = async { trips.detail(tripId) }
            when (val result = trips.itinerary(tripId)) {
                is ApiResult.Success -> itinerary = result.value
                is ApiResult.Failure -> error = result.error
            }
            (detail.await() as? ApiResult.Success)?.let { trip = it.value }
            loading = false
        }
    }
}
