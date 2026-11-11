package com.tripfair.app.ui.sample

/**
 * Fixed sample data shown while screens are not connected to the backend.
 * Values match the Figma design.
 */
/** The sample account, for previews. */
val SampleAccount = com.tripfair.app.data.auth.Account(SampleData.USER_NAME, SampleData.USER_EMAIL, SampleData.USER_PHONE)

object SampleData {
    const val USER_NAME = "Duy Đức"
    const val USER_EMAIL = "doduyduc.1102@gmail.com"
    const val USER_PHONE = "0901234567"
    /** Meets the password rule: at least 8 characters with letters and digits. */
    const val PASSWORD = "tripfair26"
    const val OTP_CODE = "482915"
}
