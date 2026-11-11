package com.tripfair.app.ui.screens.profile

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.R
import com.tripfair.app.data.auth.AuthRepository
import com.tripfair.app.ui.screens.auth.message
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val name: String = "",
    /** Shown only: changing them needs a code sent to the new address, which comes later. */
    val email: String? = null,
    val phone: String? = null,
    @param:StringRes val nameError: Int? = null,
    val loading: Boolean = false,
    val saved: Boolean = false,
)

/** Changes the account's name on the backend (PATCH /me). */
class EditProfileViewModel(private val auth: AuthRepository) : ViewModel() {
    var state by mutableStateOf(EditProfileUiState())
        private set

    init {
        auth.currentAccount.value?.let {
            state = state.copy(name = it.fullName, email = it.email, phone = it.phone)
        }
    }

    fun onNameChange(value: String) {
        state = state.copy(name = value, nameError = null)
    }

    fun save() {
        if (state.loading) return
        if (state.name.isBlank()) {
            state = state.copy(nameError = R.string.auth_error_name_required)
            return
        }
        viewModelScope.launch {
            state = state.copy(loading = true)
            val error = auth.updateName(state.name)
            state = if (error == null) {
                state.copy(loading = false, saved = true)
            } else {
                state.copy(loading = false, nameError = error.message())
            }
        }
    }
}
