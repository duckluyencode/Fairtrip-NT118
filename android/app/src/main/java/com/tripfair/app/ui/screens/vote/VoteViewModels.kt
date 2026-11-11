package com.tripfair.app.ui.screens.vote

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.places.PlaceDetailDto
import com.tripfair.app.data.places.PlacesRepository
import com.tripfair.app.data.trips.TripDetailDto
import com.tripfair.app.data.trips.TripSummaryDto
import com.tripfair.app.data.trips.TripsRepository
import com.tripfair.app.data.trips.VoteOptionDto
import com.tripfair.app.data.trips.VoteOptionListDto
import com.tripfair.app.ui.screens.explore.loadErrorMessage
import com.tripfair.app.ui.screens.trip.tripErrorMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * A group's vote: the places, who agreed, and your own ballot. Each tick is saved at once;
 * sending the ballot locks your votes (the backend refuses changes after that).
 */
class VotesViewModel(private val trips: TripsRepository) : ViewModel() {
    var trip by mutableStateOf<TripDetailDto?>(null)
        private set
    var votes by mutableStateOf<VoteOptionListDto?>(null)
        private set
    var loading by mutableStateOf(true)
        private set

    /** A vote or the ballot is being saved. */
    var busy by mutableStateOf(false)
        private set

    @get:StringRes
    var error by mutableStateOf<Int?>(null)
        private set

    private var tripId = 0

    /** Each time the screen shows: other members vote meanwhile. */
    fun start(tripId: Int) {
        this.tripId = tripId
        load()
    }

    fun load() {
        viewModelScope.launch {
            loading = votes == null
            val detail = async { trips.detail(tripId) }
            show(trips.options(tripId))
            (detail.await() as? ApiResult.Success)?.let { trip = it.value }
            loading = false
        }
    }

    fun toggle(option: VoteOptionDto) = change { trips.setVote(tripId, option.id, !option.votedByMe) }

    fun send() = change { trips.sendBallot(tripId) }

    private fun change(request: suspend () -> ApiResult<VoteOptionListDto>) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            show(request())
            busy = false
        }
    }

    private fun show(result: ApiResult<VoteOptionListDto>) {
        when (result) {
            is ApiResult.Success -> {
                votes = result.value
                error = null
            }
            is ApiResult.Failure -> error = tripErrorMessage(result.error)
        }
    }
}

/** "Thêm vào bình chọn": puts a place on the vote list of one of the user's groups. */
class AddToVoteViewModel(private val places: PlacesRepository, private val trips: TripsRepository) : ViewModel() {
    var place by mutableStateOf<PlaceDetailDto?>(null)
        private set

    /** The user's group trips; a place can be added to any of them. */
    var groups by mutableStateOf<List<TripSummaryDto>>(emptyList())
        private set
    var selectedTripId by mutableStateOf<Int?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var saving by mutableStateOf(false)
        private set

    @get:StringRes
    var error by mutableStateOf<Int?>(null)
        private set

    /** The trip the place was added to; the screen then shows that trip's vote list. */
    var addedTo by mutableStateOf<Int?>(null)
        private set

    private var placeId: Int? = null

    fun start(placeId: Int) {
        if (this.placeId == placeId) return
        this.placeId = placeId
        load()
    }

    fun load() {
        val id = placeId ?: return
        viewModelScope.launch {
            loading = true
            error = null
            val list = async { trips.list() }
            when (val detail = places.detail(id, sessionToken = null)) {
                is ApiResult.Success -> place = detail.value
                is ApiResult.Failure -> error = loadErrorMessage(detail.error)
            }
            when (val result = list.await()) {
                is ApiResult.Success -> {
                    groups = result.value.filter { it.isGroup }
                    selectedTripId = selectedTripId ?: groups.firstOrNull()?.id
                }
                is ApiResult.Failure -> error = tripErrorMessage(result.error)
            }
            loading = false
        }
    }

    fun select(tripId: Int) {
        selectedTripId = tripId
        error = null
    }

    fun add() {
        val trip = selectedTripId ?: return
        val id = placeId ?: return
        if (saving) return
        viewModelScope.launch {
            saving = true
            when (val result = trips.addOption(trip, id)) {
                is ApiResult.Success -> addedTo = trip
                is ApiResult.Failure -> error = tripErrorMessage(result.error)
            }
            saving = false
        }
    }

    fun onNavigated() {
        addedTo = null
    }
}
