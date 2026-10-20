package com.tripfair.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// One route per screen. @Serializable lets the back stack survive rotation and process death.

@Serializable
data object Login : NavKey

@Serializable
data object Register : NavKey

@Serializable
data object ForgotPassword : NavKey

/** Why the code was sent: decides where "Xác thực mã" goes next. */
@Serializable
enum class OtpPurpose { Register, ResetPassword }

@Serializable
data class OtpVerification(val purpose: OtpPurpose) : NavKey

@Serializable
data object NewPassword : NavKey

@Serializable
data object Home : NavKey
