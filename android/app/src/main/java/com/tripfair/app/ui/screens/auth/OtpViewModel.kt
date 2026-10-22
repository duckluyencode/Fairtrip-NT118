package com.tripfair.app.ui.screens.auth

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.R
import com.tripfair.app.data.auth.AuthRepository
import com.tripfair.app.data.auth.AuthValidation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A new code can be requested this long after the last one was sent. */
private const val RESEND_SECONDS = 60

data class OtpUiState(
    /** Email or phone number the code was sent to. */
    val destination: String = "",
    val code: String = "",
    @param:StringRes val codeError: Int? = null,
    val resendInSeconds: Int = RESEND_SECONDS,
    val loading: Boolean = false,
    val verified: Boolean = false,
)

/** Checks the 6-digit code and counts down until "Gửi lại mã" can be used. */
class OtpViewModel(
    private val auth: AuthRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    // Compose state, not a Flow: Compose recommends synchronous state for text fields so typing is never applied to a stale value.
    var state by mutableStateOf(OtpUiState())
        private set

    private var countdown: Job? = null

    init {
        viewModelScope.launch {
            auth.pendingCode.collect { pending ->
                // After a successful registration the code is cleared; keep showing where it went.
                if (pending != null) {
                    state = state.copy(destination = pending.contact)
                    startCountdown(pending.sentAtMillis)
                }
            }
        }
    }

    fun onCodeChange(value: String) {
        state = state.copy(code = value.filter(Char::isDigit).take(AuthValidation.CODE_LENGTH), codeError = null)
    }

    fun verify() {
        if (state.loading) return
        if (!AuthValidation.isCode(state.code)) {
            state = state.copy(codeError = R.string.auth_error_code_incomplete)
            return
        }
        viewModelScope.launch {
            state = state.copy(loading = true)
            val error = auth.verifyCode(state.code)
            state = if (error == null) state.copy(loading = false, verified = true) else state.copy(loading = false, codeError = error.message())
        }
    }

    fun resend() {
        if (state.resendInSeconds > 0) return
        viewModelScope.launch {
            val error = auth.resendCode()
            state = state.copy(codeError = error?.message())
        }
    }

    fun onVerifiedHandled() {
        state = state.copy(verified = false)
    }

    private fun startCountdown(sentAtMillis: Long) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            while (true) {
                val elapsed = ((clock() - sentAtMillis) / 1000).toInt()
                val left = (RESEND_SECONDS - elapsed).coerceAtLeast(0)
                state = state.copy(resendInSeconds = left)
                if (left == 0) break
                delay(1000)
            }
        }
    }
}
