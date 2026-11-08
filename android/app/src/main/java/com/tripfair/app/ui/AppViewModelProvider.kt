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
import com.tripfair.app.ui.screens.explore.PlaceDetailViewModel
import com.tripfair.app.ui.screens.explore.SearchHistoryViewModel
import com.tripfair.app.ui.screens.explore.SearchResultsViewModel
import com.tripfair.app.ui.screens.explore.SearchViewModel
import com.tripfair.app.ui.screens.home.HomeViewModel
import com.tripfair.app.ui.screens.profile.DeleteAccountViewModel
import com.tripfair.app.ui.screens.profile.FavoritesViewModel
import com.tripfair.app.ui.screens.profile.EditProfileViewModel
import com.tripfair.app.ui.screens.sos.ContactFormViewModel
import com.tripfair.app.ui.screens.sos.EmergencyContactsViewModel

/** Creates every ViewModel with the repositories it needs from the [AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { LoginViewModel(container().authRepository) }
        initializer { RegisterViewModel(container().authRepository) }
        initializer { ForgotPasswordViewModel(container().authRepository) }
        initializer { OtpViewModel(container().authRepository) }
        initializer { NewPasswordViewModel(container().authRepository) }
        initializer { EditProfileViewModel(container().authRepository) }
        initializer { DeleteAccountViewModel(container().authRepository) }
        initializer { HomeViewModel(container().placesRepository) }
        initializer { SearchViewModel(container().placesRepository) }
        initializer { SearchResultsViewModel(container().placesRepository) }
        initializer { PlaceDetailViewModel(container().placesRepository) }
        initializer { SearchHistoryViewModel(container().placesRepository) }
        initializer { FavoritesViewModel(container().placesRepository) }
        initializer { EmergencyContactsViewModel(container().emergencyContactsRepository) }
        initializer { ContactFormViewModel(container().emergencyContactsRepository) }
    }
}

private fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as TripFairApplication).container
