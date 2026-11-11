package com.tripfair.app.ui.screens.trip

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.R
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.trips.TripDraft
import com.tripfair.app.data.trips.TripsRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

/** The backend plans at most 14 days after the first one. */
const val MAX_TRIP_DAYS = 15

data class CreateTripUiState(
    val group: Boolean = true,
    val city: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    /** Digits only; empty means 0đ. */
    val budget: String = "",
    val suggestLodging: Boolean = true,
    @param:StringRes val cityError: Int? = null,
    @param:StringRes val datesError: Int? = null,
    @param:StringRes val error: Int? = null,
    val loading: Boolean = false,
)

/** Where "Tạo chuyến đi" goes after its button. */
sealed interface CreateTripNext {
    /** "Cùng nhóm": name the group next. */
    data class NameGroup(val draft: TripDraft) : CreateTripNext

    /** A solo trip was created and wants hotel suggestions. */
    data class ChooseLodging(val tripId: Int) : CreateTripNext

    /** A solo trip was created with "Tự lo chỗ ở": plan it now. */
    data class Plan(val tripId: Int) : CreateTripNext
}

class CreateTripViewModel(private val trips: TripsRepository) : ViewModel() {
    var state by mutableStateOf(CreateTripUiState())
        private set

    /** Set once the form is done; the screen navigates, then calls [onNavigated]. */
    var next by mutableStateOf<CreateTripNext?>(null)
        private set

    fun onGroupChange(group: Boolean) {
        state = state.copy(group = group, error = null)
    }

    fun onCityChange(city: String) {
        state = state.copy(city = city, cityError = null, error = null)
    }

    fun onDatesChange(start: LocalDate, end: LocalDate) {
        state = state.copy(startDate = start, endDate = end, datesError = null, error = null)
    }

    fun onBudgetChange(digits: String) {
        state = state.copy(budget = digits, error = null)
    }

    fun onLodgingChange(suggest: Boolean) {
        state = state.copy(suggestLodging = suggest)
    }

    fun onNavigated() {
        next = null
    }

    fun submit() {
        if (state.loading) return
        val draft = validDraft() ?: return
        if (state.group) {
            next = CreateTripNext.NameGroup(draft)
            return
        }
        viewModelScope.launch {
            state = state.copy(loading = true, error = null)
            when (val result = trips.create(draft.request(group = false))) {
                is ApiResult.Success -> {
                    val id = result.value.id
                    next = if (draft.wantsHotel(group = false)) CreateTripNext.ChooseLodging(id) else CreateTripNext.Plan(id)
                    state = state.copy(loading = false)
                }
                is ApiResult.Failure -> state = state.copy(loading = false, error = tripErrorMessage(result.error))
            }
        }
    }

    private fun validDraft(): TripDraft? {
        val city = state.city.trim()
        val start = state.startDate
        val end = state.endDate
        val days = if (start != null && end != null) ChronoUnit.DAYS.between(start, end).toInt() + 1 else 0
        val datesError = when {
            start == null || end == null -> R.string.trip_error_dates_missing
            days > MAX_TRIP_DAYS -> R.string.trip_error_dates_too_long
            else -> null
        }
        val cityError = if (city.isEmpty()) R.string.trip_error_city_missing else null
        if (cityError != null || datesError != null) {
            state = state.copy(cityError = cityError, datesError = datesError)
            return null
        }
        if (start == null || end == null) return null
        return TripDraft(
            city = city,
            startDate = start.toString(),
            endDate = end.toString(),
            days = days,
            budget = state.budget.toLongOrNull() ?: 0L,
            hotel = state.suggestLodging,
        )
    }
}
