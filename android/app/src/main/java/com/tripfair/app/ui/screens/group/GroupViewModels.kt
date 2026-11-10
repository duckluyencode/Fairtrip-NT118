package com.tripfair.app.ui.screens.group

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.trips.TripDetailDto
import com.tripfair.app.data.trips.TripsRepository
import com.tripfair.app.ui.screens.trip.tripErrorMessage
import kotlinx.coroutines.launch

/**
 * One trip with its members, for "Lập kế hoạch nhóm" and "Mời bạn". [start] runs each time
 * the screen shows, so members who joined meanwhile appear.
 */
class TripDetailViewModel(private val trips: TripsRepository) : ViewModel() {
    var trip by mutableStateOf<TripDetailDto?>(null)
        private set
    var loading by mutableStateOf(true)
        private set

    @get:StringRes
    var error by mutableStateOf<Int?>(null)
        private set

    private var tripId = 0

    fun start(tripId: Int) {
        this.tripId = tripId
        load()
    }

    fun load() {
        viewModelScope.launch {
            loading = trip == null
            when (val result = trips.detail(tripId)) {
                is ApiResult.Success -> {
                    trip = result.value
                    error = null
                }
                is ApiResult.Failure -> error = tripErrorMessage(result.error)
            }
            loading = false
        }
    }
}

/**
 * "Chỉnh ngân sách & sở thích": your own budget and interests in a group. Also the last step
 * of joining, where they are sent together with the invite code.
 */
class EditBudgetViewModel(private val trips: TripsRepository) : ViewModel() {
    /** Digits only; empty means 0đ. */
    var budget by mutableStateOf("")
        private set
    var interests by mutableStateOf<Set<String>>(emptySet())
        private set

    /** The group's name once known; joining only knows the code. */
    var groupName by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set

    @get:StringRes
    var error by mutableStateOf<Int?>(null)
        private set

    /** The trip saved to or joined; the screen leaves when it is set. */
    var savedTripId by mutableStateOf<Int?>(null)
        private set

    private var tripId: Int? = null
    private var inviteCode: String? = null

    /** Editing your part of [tripId]: starts from what you set before. */
    fun startEdit(tripId: Int) {
        if (this.tripId == tripId) return
        this.tripId = tripId
        viewModelScope.launch {
            loading = true
            when (val result = trips.detail(tripId)) {
                is ApiResult.Success -> {
                    val trip = result.value
                    groupName = trip.name
                    trip.me?.let { me ->
                        budget = me.budget?.takeIf { it > 0 }?.toString().orEmpty()
                        interests = me.interests.toSet()
                    }
                }
                is ApiResult.Failure -> error = tripErrorMessage(result.error)
            }
            loading = false
        }
    }

    /** Joining with [code]: nothing to load. */
    fun startJoin(code: String) {
        inviteCode = code
    }

    fun onBudgetChange(digits: String) {
        budget = digits
        error = null
    }

    fun toggle(interest: String) {
        interests = if (interest in interests) interests - interest else interests + interest
        error = null
    }

    fun save() {
        if (saving || loading) return
        val amount = budget.toLongOrNull() ?: 0L
        val chosen = interests.toList()
        viewModelScope.launch {
            saving = true
            error = null
            val code = inviteCode
            val id = tripId
            val result = when {
                code != null -> trips.join(code, amount, chosen)
                id != null -> trips.updateMe(id, amount, chosen)
                else -> null
            }
            when (result) {
                is ApiResult.Success -> savedTripId = result.value.id
                is ApiResult.Failure -> error = tripErrorMessage(result.error)
                null -> Unit
            }
            saving = false
        }
    }

    fun onNavigated() {
        savedTripId = null
    }
}
