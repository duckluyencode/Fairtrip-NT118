package com.tripfair.app.data.auth

import com.tripfair.app.ui.sample.SampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory accounts until the backend exists. Starts with the sample account; every code
 * sent is [SAMPLE_CODE]. Data is lost when the app process ends.
 */
class SampleAuthRepository(
    private val clock: () -> Long = System::currentTimeMillis,
) : AuthRepository {

    private class StoredAccount(var account: Account, var password: String) {
        fun matches(contact: String) = contact == account.email || contact == account.phone
    }

    private val accounts = mutableListOf(
        StoredAccount(Account(SampleData.USER_NAME, SampleData.USER_EMAIL, SampleData.USER_PHONE), SampleData.PASSWORD),
    )

    /** The account being registered, added to [accounts] once its code is verified. */
    private var registering: StoredAccount? = null

    /** The account whose password is being reset, and whether its code was verified. */
    private var resetting: StoredAccount? = null
    private var resetVerified = false

    private val _currentAccount = MutableStateFlow<Account?>(null)
    override val currentAccount: StateFlow<Account?> = _currentAccount.asStateFlow()

    private val _pendingCode = MutableStateFlow<PendingCode?>(null)
    override val pendingCode: StateFlow<PendingCode?> = _pendingCode.asStateFlow()

    override suspend fun login(contact: String, password: String): AuthError? {
        val stored = find(contact)
        if (stored == null || stored.password != password) return AuthError.WrongCredentials
        _currentAccount.value = stored.account
        return null
    }

    override suspend fun startRegistration(fullName: String, contact: String, password: String): AuthError? {
        val normalized = AuthValidation.normalizeContact(contact)
        if (find(normalized) != null) return AuthError.AccountExists
        val isEmail = AuthValidation.isEmail(normalized)
        registering = StoredAccount(
            Account(
                fullName = fullName.trim(),
                email = normalized.takeIf { isEmail },
                phone = normalized.takeUnless { isEmail },
            ),
            password,
        )
        resetting = null
        sendCode(CodePurpose.Register, normalized)
        return null
    }

    override suspend fun startPasswordReset(contact: String): AuthError? {
        val normalized = AuthValidation.normalizeContact(contact)
        val stored = find(normalized) ?: return AuthError.AccountNotFound
        resetting = stored
        resetVerified = false
        registering = null
        sendCode(CodePurpose.ResetPassword, normalized)
        return null
    }

    override suspend fun resendCode(): AuthError? {
        val pending = _pendingCode.value ?: return AuthError.NoPendingCode
        sendCode(pending.purpose, pending.contact)
        return null
    }

    override suspend fun verifyCode(code: String): AuthError? {
        val pending = _pendingCode.value ?: return AuthError.NoPendingCode
        if (clock() - pending.sentAtMillis > CODE_VALID_MILLIS) return AuthError.CodeExpired
        if (code != SAMPLE_CODE) return AuthError.WrongCode
        when (pending.purpose) {
            CodePurpose.Register -> {
                val stored = registering ?: return AuthError.NoPendingCode
                accounts += stored
                registering = null
                _pendingCode.value = null
                _currentAccount.value = stored.account
            }
            // The code stays pending so the new-password screen can show where it was sent.
            CodePurpose.ResetPassword -> resetVerified = true
        }
        return null
    }

    override suspend fun setNewPassword(password: String): AuthError? {
        val stored = resetting?.takeIf { resetVerified } ?: return AuthError.NoPendingCode
        stored.password = password
        resetting = null
        resetVerified = false
        _pendingCode.value = null
        return null
    }

    override fun logout() {
        _currentAccount.value = null
    }

    private fun find(contact: String): StoredAccount? {
        val normalized = AuthValidation.normalizeContact(contact)
        return accounts.firstOrNull { it.matches(normalized) }
    }

    private fun sendCode(purpose: CodePurpose, contact: String) {
        _pendingCode.value = PendingCode(purpose, contact, sentAtMillis = clock())
    }

    companion object {
        /** Every sample code is the same, so it can be typed while testing. */
        const val SAMPLE_CODE = SampleData.OTP_CODE

        /** "Mã có hiệu lực trong 15 phút." */
        const val CODE_VALID_MILLIS = 15 * 60 * 1000L
    }
}
