package com.tripfair.app.data.auth

import kotlinx.coroutines.flow.StateFlow

/** A signed-in user. The password never leaves the repository. */
data class Account(val fullName: String, val email: String?, val phone: String?) {
    /** The email, or the phone number for phone-only accounts. */
    val contact: String get() = email ?: phone.orEmpty()
}

/** Why an account request failed, so the screen can show the right message. */
enum class AuthError {
    WrongCredentials,
    AccountExists,
    AccountNotFound,
    WrongCode,
    CodeExpired,

    /** No code is waiting to be checked, e.g. the registration or reset was not started. */
    NoPendingCode,

    /** Too many wrong codes: a new code has to be requested. */
    CodeLocked,

    /** A new code was asked for less than a minute after the last one. */
    ResendTooSoon,

    /** The email or SMS with the code could not be sent. */
    CodeDeliveryFailed,

    /** Too many attempts from this device in a short time. */
    TooManyRequests,

    /** No internet, or the server did not answer. */
    Network,

    /** Anything else the server reported. */
    Unexpected,
}

enum class CodePurpose { Register, ResetPassword }

/** A 6-digit code sent to [contact] that has not been used yet. */
data class PendingCode(val purpose: CodePurpose, val contact: String, val sentAtMillis: Long)

/**
 * Accounts, sign-in and the codes sent by email or SMS. Functions return null on success,
 * or the reason they failed.
 */
interface AuthRepository {
    val currentAccount: StateFlow<Account?>
    val pendingCode: StateFlow<PendingCode?>

    suspend fun login(contact: String, password: String): AuthError?

    /** Checks the contact is free and sends a code; the account is created once the code is verified. */
    suspend fun startRegistration(fullName: String, contact: String, password: String): AuthError?

    /** Sends a code to an existing account so its password can be reset. */
    suspend fun startPasswordReset(contact: String): AuthError?

    /** Sends a new code for the pending registration or reset. */
    suspend fun resendCode(): AuthError?

    /** Registration: creates the account and signs in. Reset: allows [setNewPassword]. */
    suspend fun verifyCode(code: String): AuthError?

    suspend fun setNewPassword(password: String): AuthError?

    /** Loads the signed-in account again (e.g. at app start, from a saved session). */
    suspend fun refreshAccount(): AuthError?

    suspend fun updateName(fullName: String): AuthError?

    /** Deletes the account after checking [password]; the session ends on success. */
    suspend fun deleteAccount(password: String): AuthError?

    fun logout()
}
