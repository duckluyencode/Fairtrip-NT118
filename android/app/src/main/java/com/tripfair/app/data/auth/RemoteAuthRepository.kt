package com.tripfair.app.data.auth

import com.tripfair.app.data.network.ApiClient
import com.tripfair.app.data.network.ApiError
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.network.AuthApi
import com.tripfair.app.data.network.CodeSentResponse
import com.tripfair.app.data.network.ContactRequest
import com.tripfair.app.data.network.DeleteAccountRequest
import com.tripfair.app.data.network.LoginRequest
import com.tripfair.app.data.network.MeApi
import com.tripfair.app.data.network.RefreshRequest
import com.tripfair.app.data.network.RegisterRequest
import com.tripfair.app.data.network.ResendCodeRequest
import com.tripfair.app.data.network.ResetPasswordRequest
import com.tripfair.app.data.network.SessionResponse
import com.tripfair.app.data.network.UpdateProfileRequest
import com.tripfair.app.data.network.UserResponse
import com.tripfair.app.data.network.VerifyCodeRequest
import com.tripfair.app.data.network.apiCall
import com.tripfair.app.data.session.SessionManager
import com.tripfair.app.data.session.SessionTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The backend's error codes for account requests, as the app's [AuthError]. */
internal fun authErrorOf(error: ApiError): AuthError = when (error.code) {
    "WRONG_CREDENTIALS" -> AuthError.WrongCredentials
    "ACCOUNT_EXISTS" -> AuthError.AccountExists
    "ACCOUNT_NOT_FOUND" -> AuthError.AccountNotFound
    "WRONG_CODE" -> AuthError.WrongCode
    "CODE_EXPIRED" -> AuthError.CodeExpired
    "CODE_LOCKED" -> AuthError.CodeLocked
    // A reset token that expired (10 minutes) means starting the reset again.
    "NO_PENDING_CODE", "INVALID_TOKEN" -> AuthError.NoPendingCode
    "RESEND_TOO_SOON" -> AuthError.ResendTooSoon
    "CODE_DELIVERY_FAILED" -> AuthError.CodeDeliveryFailed
    "TOO_MANY_REQUESTS" -> AuthError.TooManyRequests
    ApiError.NETWORK -> AuthError.Network
    else -> AuthError.Unexpected
}

private fun UserResponse.toAccount() = Account(fullName = fullName, email = email, phone = phone)

private fun CodePurpose.apiName() = when (this) {
    CodePurpose.Register -> "REGISTER"
    CodePurpose.ResetPassword -> "RESET_PASSWORD"
}

/**
 * Accounts on the TripFair backend. Signing in stores the tokens in [session]; the code
 * flows keep, in memory only, where the code went and (for a reset) the reset token.
 * [scope] runs the sign-out request, which must finish even when the screen is gone.
 */
class RemoteAuthRepository(
    private val auth: AuthApi,
    private val me: MeApi,
    private val session: SessionManager,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) : AuthRepository {
    private val json = ApiClient.json

    private val _currentAccount = MutableStateFlow<Account?>(null)
    override val currentAccount: StateFlow<Account?> = _currentAccount.asStateFlow()

    private val _pendingCode = MutableStateFlow<PendingCode?>(null)
    override val pendingCode: StateFlow<PendingCode?> = _pendingCode.asStateFlow()

    /** From POST auth/password/verify; lets setNewPassword change the password once. */
    private var resetToken: String? = null

    private suspend fun <T> call(block: suspend () -> T, onSuccess: suspend (T) -> Unit): AuthError? =
        when (val result = apiCall(json, block)) {
            is ApiResult.Success -> {
                onSuccess(result.value)
                null
            }
            is ApiResult.Failure -> authErrorOf(result.error)
        }

    private suspend fun signIn(response: SessionResponse) {
        session.signIn(SessionTokens(response.accessToken, response.refreshToken))
        _currentAccount.value = response.user.toAccount()
    }

    private fun codeSent(purpose: CodePurpose, response: CodeSentResponse) {
        _pendingCode.value = PendingCode(purpose, response.sentTo, sentAtMillis = clock())
    }

    override suspend fun login(contact: String, password: String): AuthError? =
        call({ auth.login(LoginRequest(contact.trim(), password)) }) { signIn(it) }

    override suspend fun startRegistration(fullName: String, contact: String, password: String): AuthError? =
        call({ auth.register(RegisterRequest(fullName.trim(), contact.trim(), password)) }) {
            resetToken = null
            codeSent(CodePurpose.Register, it)
        }

    override suspend fun startPasswordReset(contact: String): AuthError? =
        call({ auth.forgotPassword(ContactRequest(contact.trim())) }) {
            resetToken = null
            codeSent(CodePurpose.ResetPassword, it)
        }

    override suspend fun resendCode(): AuthError? {
        val pending = _pendingCode.value ?: return AuthError.NoPendingCode
        return call({ auth.resendCode(ResendCodeRequest(pending.contact, pending.purpose.apiName())) }) {
            codeSent(pending.purpose, it)
        }
    }

    override suspend fun verifyCode(code: String): AuthError? {
        val pending = _pendingCode.value ?: return AuthError.NoPendingCode
        val request = VerifyCodeRequest(pending.contact, code)
        return when (pending.purpose) {
            CodePurpose.Register -> call({ auth.verifyRegistration(request) }) {
                _pendingCode.value = null
                signIn(it)
            }
            // The code stays pending so the new-password screen can show where it was sent.
            CodePurpose.ResetPassword -> call({ auth.verifyPasswordReset(request) }) {
                resetToken = it.resetToken
            }
        }
    }

    override suspend fun setNewPassword(password: String): AuthError? {
        val token = resetToken ?: return AuthError.NoPendingCode
        return call({ auth.resetPassword(ResetPasswordRequest(token, password)) }) {
            resetToken = null
            _pendingCode.value = null
        }
    }

    override suspend fun refreshAccount(): AuthError? =
        call({ me.profile() }) { _currentAccount.value = it.toAccount() }

    override suspend fun updateName(fullName: String): AuthError? =
        call({ me.updateProfile(UpdateProfileRequest(fullName.trim())) }) {
            _currentAccount.value = it.toAccount()
        }

    override suspend fun deleteAccount(password: String): AuthError? =
        call({ me.deleteAccount(DeleteAccountRequest(password)) }) {
            _currentAccount.value = null
            _pendingCode.value = null
            session.signOut()
        }

    /**
     * Ends the session on this phone first, so signing in again right away is never undone;
     * then the refresh token is revoked on the server (if that fails, e.g. offline, the
     * token simply expires in 30 days).
     */
    override fun logout() {
        val tokens = session.tokens
        _currentAccount.value = null
        scope.launch {
            session.signOut()
            if (tokens != null) {
                apiCall(json) { auth.logout(RefreshRequest(tokens.refreshToken)) }
            }
        }
    }
}
