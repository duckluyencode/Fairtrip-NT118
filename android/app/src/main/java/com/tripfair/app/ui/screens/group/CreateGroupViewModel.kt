package com.tripfair.app.ui.screens.group

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
import com.tripfair.app.ui.screens.trip.tripErrorMessage
import kotlinx.coroutines.launch

/** The group trip just created; [invite] when the user asked to invite friends first. */
data class CreatedGroup(val tripId: Int, val invite: Boolean)

/** "Tạo nhóm": names the group and creates the group trip from the "Tạo chuyến đi" draft. */
class CreateGroupViewModel(private val trips: TripsRepository) : ViewModel() {
    var name by mutableStateOf("")
        private set

    @get:StringRes
    var nameError by mutableStateOf<Int?>(null)
        private set

    @get:StringRes
    var error by mutableStateOf<Int?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    var created by mutableStateOf<CreatedGroup?>(null)
        private set

    fun onNameChange(value: String) {
        name = value.take(100)
        nameError = null
        error = null
    }

    fun create(draft: TripDraft, invite: Boolean) {
        if (loading) return
        if (name.isBlank()) {
            nameError = R.string.create_group_error_name
            return
        }
        viewModelScope.launch {
            loading = true
            error = null
            when (val result = trips.create(draft.request(group = true, name = name.trim()))) {
                is ApiResult.Success -> created = CreatedGroup(result.value.id, invite)
                is ApiResult.Failure -> error = tripErrorMessage(result.error)
            }
            loading = false
        }
    }

    fun onNavigated() {
        created = null
    }
}
