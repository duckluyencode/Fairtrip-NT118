package com.tripfair.app.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tripfair.app.TripFairApplication
import com.tripfair.app.data.AppContainer
import com.tripfair.app.ui.screens.auth.ForgotPasswordViewModel
import com.tripfair.app.ui.screens.auth.LoginViewModel
import com.tripfair.app.ui.screens.auth.NewPasswordViewModel
import com.tripfair.app.ui.screens.auth.OtpViewModel
import com.tripfair.app.ui.screens.auth.RegisterViewModel

/** Creates every ViewModel with the repositories it needs from the [AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { LoginViewModel(container().authRepository) }
        initializer { RegisterViewModel(container().authRepository) }
        initializer { ForgotPasswordViewModel(container().authRepository) }
        initializer { OtpViewModel(container().authRepository) }
        initializer { NewPasswordViewModel(container().authRepository) }
    }
}

private fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as TripFairApplication).container
