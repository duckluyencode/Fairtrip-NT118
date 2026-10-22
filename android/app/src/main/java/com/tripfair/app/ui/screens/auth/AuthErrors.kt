package com.tripfair.app.ui.screens.auth

import androidx.annotation.StringRes
import com.tripfair.app.R
import com.tripfair.app.data.auth.AuthError
import com.tripfair.app.data.auth.AuthValidation

// Field checks shared by the account screens; each returns the message to show, or null if the input is fine.

@StringRes
internal fun contactError(contact: String): Int? = when {
    contact.isBlank() -> R.string.auth_error_contact_required
    !AuthValidation.isContact(contact) -> R.string.auth_error_contact_invalid
    else -> null
}

@StringRes
internal fun newPasswordError(password: String): Int? = when {
    password.isEmpty() -> R.string.auth_error_password_required
    !AuthValidation.isPassword(password) -> R.string.auth_error_password_rule
    else -> null
}

@StringRes
internal fun confirmPasswordError(password: String, confirm: String): Int? = when {
    confirm.isEmpty() -> R.string.auth_error_confirm_required
    confirm != password -> R.string.auth_error_password_mismatch
    else -> null
}

@StringRes
internal fun AuthError.message(): Int = when (this) {
    AuthError.WrongCredentials -> R.string.auth_error_wrong_credentials
    AuthError.AccountExists -> R.string.auth_error_account_exists
    AuthError.AccountNotFound -> R.string.auth_error_account_not_found
    AuthError.WrongCode -> R.string.auth_error_code_wrong
    AuthError.CodeExpired -> R.string.auth_error_code_expired
    AuthError.NoPendingCode -> R.string.auth_error_session
}
