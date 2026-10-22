package com.tripfair.app.ui.screens.auth

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.R
import com.tripfair.app.data.auth.AuthRepository
import kotlinx.coroutines.launch

data class RegisterUiState(
    val fullName: String = "",
    val contact: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val acceptedTerms: Boolean = false,
    @param:StringRes val fullNameError: Int? = null,
    @param:StringRes val contactError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    @param:StringRes val confirmPasswordError: Int? = null,
    @param:StringRes val termsError: Int? = null,
    val loading: Boolean = false,
    /** True once the code is sent; the screen then moves on to the code screen. */
    val codeSent: Boolean = false,
)

/** Checks every field, then asks the repository to send a code to the email or phone number. */
class RegisterViewModel(private val auth: AuthRepository) : ViewModel() {
    // Compose state, not a Flow: Compose recommends synchronous state for text fields so typing is never applied to a stale value.
    var state by mutableStateOf(RegisterUiState())
        private set

    private var showErrors = false

    fun onFullNameChange(value: String) = edit { it.copy(fullName = value) }

    fun onContactChange(value: String) = edit { it.copy(contact = value) }

    fun onPasswordChange(value: String) = edit { it.copy(password = value) }

    fun onConfirmPasswordChange(value: String) = edit { it.copy(confirmPassword = value) }

    fun onAcceptedTermsChange(value: Boolean) = edit { it.copy(acceptedTerms = value) }

    fun submit() {
        if (state.loading) return
        showErrors = true
        if (!validate()) return
        viewModelScope.launch {
            state = state.copy(loading = true)
            val error = auth.startRegistration(state.fullName, state.contact, state.password)
            // The only expected failure is an email or phone number that already has an account.
            state = if (error == null) state.copy(loading = false, codeSent = true) else state.copy(loading = false, contactError = error.message())
        }
    }

    /** Called once the screen has moved on, so coming back does not move on again. */
    fun onCodeSentHandled() {
        state = state.copy(codeSent = false)
    }

    private fun edit(change: (RegisterUiState) -> RegisterUiState) {
        state = change(state)
        if (showErrors) validate()
    }

    private fun validate(): Boolean {
        state = state.copy(
            fullNameError = if (state.fullName.isBlank()) R.string.auth_error_name_required else null,
            contactError = contactError(state.contact),
            passwordError = newPasswordError(state.password),
            confirmPasswordError = confirmPasswordError(state.password, state.confirmPassword),
            termsError = if (state.acceptedTerms) null else R.string.auth_error_terms_required,
        )
        return listOf(state.fullNameError, state.contactError, state.passwordError, state.confirmPasswordError, state.termsError)
            .all { it == null }
    }
}
