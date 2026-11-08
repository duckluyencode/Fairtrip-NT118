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

data class DeleteAccountUiState(
    val password: String = "",
    @param:StringRes val passwordError: Int? = null,
    val loading: Boolean = false,
    val deleted: Boolean = false,
)

/** "Xóa tài khoản": the password is asked again; on success the session ends. */
class DeleteAccountViewModel(private val auth: AuthRepository) : ViewModel() {
    var state by mutableStateOf(DeleteAccountUiState())
        private set

    fun onPasswordChange(value: String) {
        state = state.copy(password = value, passwordError = null)
    }

    fun delete() {
        if (state.loading) return
        if (state.password.isEmpty()) {
            state = state.copy(passwordError = R.string.auth_error_password_required)
            return
        }
        viewModelScope.launch {
            state = state.copy(loading = true)
            val error = auth.deleteAccount(state.password)
            state = if (error == null) {
                state.copy(loading = false, deleted = true)
            } else {
                state.copy(loading = false, passwordError = error.message())
            }
        }
    }
}
