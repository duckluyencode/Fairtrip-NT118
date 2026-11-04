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

data class LoginUiState(
    val contact: String = "",
    val password: String = "",
    val rememberMe: Boolean = true,
    @param:StringRes val contactError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    val loading: Boolean = false,
    val loggedIn: Boolean = false,
)

/**
 * Errors show after the first tap on "Đăng nhập" and then follow the typing. A wrong
 * password is reported under the password field.
 */
class LoginViewModel(private val auth: AuthRepository) : ViewModel() {
    // Compose state, not a Flow: Compose recommends synchronous state for text fields so typing is never applied to a stale value.
    var state by mutableStateOf(LoginUiState())
        private set

    private var showErrors = false

    fun onContactChange(value: String) = edit { it.copy(contact = value) }

    fun onPasswordChange(value: String) = edit { it.copy(password = value) }

    fun onRememberMeChange(value: Boolean) {
        state = state.copy(rememberMe = value)
    }

    fun login() {
        if (state.loading) return
        showErrors = true
        if (!validate()) return
        viewModelScope.launch {
            state = state.copy(loading = true)
            val error = auth.login(state.contact, state.password)
            state = if (error == null) state.copy(loading = false, loggedIn = true) else state.copy(loading = false, passwordError = error.message())
        }
    }

    private fun edit(change: (LoginUiState) -> LoginUiState) {
        state = change(state)
        if (showErrors) validate()
    }

    private fun validate(): Boolean {
        val contactError = contactError(state.contact)
        val passwordError = if (state.password.isEmpty()) R.string.auth_error_password_required else null
        state = state.copy(contactError = contactError, passwordError = passwordError)
        return contactError == null && passwordError == null
    }
}
