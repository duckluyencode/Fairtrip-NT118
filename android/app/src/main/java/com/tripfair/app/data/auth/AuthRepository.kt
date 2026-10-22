package com.tripfair.app.data.auth

import kotlinx.coroutines.flow.StateFlow

/** A signed-in user. The password never leaves the repository. */
data class Account(val fullName: String, val email: String?, val phone: String?)

/** Why an account request failed, so the screen can show the right message. */
enum class AuthError {
    WrongCredentials,
    AccountExists,
    AccountNotFound,
    WrongCode,
    CodeExpired,

    /** No code is waiting to be checked, e.g. the registration or reset was not started. */
    NoPendingCode,
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

    fun logout()
}
