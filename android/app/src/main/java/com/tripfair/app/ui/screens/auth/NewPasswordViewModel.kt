package com.tripfair.app.ui.screens.auth

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.data.auth.AuthRepository
import kotlinx.coroutines.launch

data class NewPasswordUiState(
    /** The verified email or phone number, shown under "Tài khoản đã được xác thực". */
    val contact: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    @param:StringRes val passwordError: Int? = null,
    @param:StringRes val confirmPasswordError: Int? = null,
    val loading: Boolean = false,
    val saved: Boolean = false,
)

/** Saves the new password of the account whose reset code was verified. */
class NewPasswordViewModel(private val auth: AuthRepository) : ViewModel() {
    // Compose state, not a Flow: Compose recommends synchronous state for text fields so typing is never applied to a stale value.
    var state by mutableStateOf(NewPasswordUiState(contact = auth.pendingCode.value?.contact.orEmpty()))
        private set

    private var showErrors = false

    fun onPasswordChange(value: String) = edit { it.copy(password = value) }

    fun onConfirmPasswordChange(value: String) = edit { it.copy(confirmPassword = value) }

    fun save() {
        if (state.loading) return
        showErrors = true
        if (!validate()) return
        viewModelScope.launch {
            state = state.copy(loading = true)
            val error = auth.setNewPassword(state.password)
            state = if (error == null) state.copy(loading = false, saved = true) else state.copy(loading = false, passwordError = error.message())
        }
    }

    private fun edit(change: (NewPasswordUiState) -> NewPasswordUiState) {
        state = change(state)
        if (showErrors) validate()
    }

    private fun validate(): Boolean {
        val passwordError = newPasswordError(state.password)
        val confirmError = confirmPasswordError(state.password, state.confirmPassword)
        state = state.copy(passwordError = passwordError, confirmPasswordError = confirmError)
        return passwordError == null && confirmError == null
    }
}
