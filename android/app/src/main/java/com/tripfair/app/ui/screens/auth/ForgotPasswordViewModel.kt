package com.tripfair.app.ui.screens.auth

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.data.auth.AuthRepository
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val contact: String = "",
    @param:StringRes val contactError: Int? = null,
    val loading: Boolean = false,
    val codeSent: Boolean = false,
)

/** Sends a reset code, but only to an email or phone number that has an account. */
class ForgotPasswordViewModel(private val auth: AuthRepository) : ViewModel() {
    // Compose state, not a Flow: Compose recommends synchronous state for text fields so typing is never applied to a stale value.
    var state by mutableStateOf(ForgotPasswordUiState())
        private set

    private var showErrors = false

    fun onContactChange(value: String) {
        state = state.copy(contact = value)
        if (showErrors) validate()
    }

    fun sendCode() {
        if (state.loading) return
        showErrors = true
        if (!validate()) return
        viewModelScope.launch {
            state = state.copy(loading = true)
            val error = auth.startPasswordReset(state.contact)
            state = if (error == null) state.copy(loading = false, codeSent = true) else state.copy(loading = false, contactError = error.message())
        }
    }

    fun onCodeSentHandled() {
        state = state.copy(codeSent = false)
    }

    private fun validate(): Boolean {
        val error = contactError(state.contact)
        state = state.copy(contactError = error)
        return error == null
    }
}
