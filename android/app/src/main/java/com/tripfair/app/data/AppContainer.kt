package com.tripfair.app.data

import com.tripfair.app.data.auth.AuthRepository
import com.tripfair.app.data.auth.SampleAuthRepository

/**
 * Creates the repositories once for the whole app. Screens get them through their ViewModels.
 * Swapping a sample repository for a backend one only changes this file.
 */
class AppContainer {
    val authRepository: AuthRepository = SampleAuthRepository()
}
