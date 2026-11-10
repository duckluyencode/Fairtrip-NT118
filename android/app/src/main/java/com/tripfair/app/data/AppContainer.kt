package com.tripfair.app.data

import android.content.Context
import com.tripfair.app.BuildConfig
import com.tripfair.app.data.auth.AuthRepository
import com.tripfair.app.data.auth.RemoteAuthRepository
import com.tripfair.app.data.contacts.DataStoreContactsCache
import com.tripfair.app.data.contacts.EmergencyContactsApi
import com.tripfair.app.data.contacts.EmergencyContactsRepository
import com.tripfair.app.data.contacts.RemoteEmergencyContactsRepository
import com.tripfair.app.data.contacts.contactsDataStore
import com.tripfair.app.data.network.ApiClient
import com.tripfair.app.data.network.AuthApi
import com.tripfair.app.data.network.MeApi
import com.tripfair.app.data.places.PlacesApi
import com.tripfair.app.data.places.PlacesRepository
import com.tripfair.app.data.places.RemotePlacesRepository
import com.tripfair.app.data.session.AuthInterceptor
import com.tripfair.app.data.session.DataStoreSessionStore
import com.tripfair.app.data.session.KeystoreTokenCipher
import com.tripfair.app.data.session.RefreshApi
import com.tripfair.app.data.session.SessionManager
import com.tripfair.app.data.session.TokenAuthenticator
import com.tripfair.app.data.session.SessionState
import com.tripfair.app.data.session.sessionDataStore
import com.tripfair.app.data.trips.RemoteTripsRepository
import com.tripfair.app.data.trips.TripsApi
import com.tripfair.app.data.trips.TripsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.Retrofit

/**
 * Creates the repositories once for the whole app. Screens get them through their ViewModels.
 * Swapping a sample repository for a backend one only changes this file.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Work that outlives any screen (restoring the session, signing out). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val session = SessionManager(
        DataStoreSessionStore(appContext.sessionDataStore, KeystoreTokenCipher()),
    )

    // Sign-in routes and the token refresh go without the access token and its refresh logic.
    private val publicRetrofit: Retrofit by lazy {
        ApiClient.retrofit(BuildConfig.API_BASE_URL, ApiClient.httpClient())
    }

    // Everything else: the access token is added, and refreshed on 401.
    private val retrofit: Retrofit by lazy {
        val client = ApiClient.httpClient(
            interceptors = listOf(AuthInterceptor(session)),
            authenticator = TokenAuthenticator(session, publicRetrofit.create(RefreshApi::class.java)),
        )
        ApiClient.retrofit(BuildConfig.API_BASE_URL, client)
    }

    val authApi: AuthApi by lazy { publicRetrofit.create(AuthApi::class.java) }
    val meApi: MeApi by lazy { retrofit.create(MeApi::class.java) }

    val placesRepository: PlacesRepository by lazy {
        RemotePlacesRepository(retrofit.create(PlacesApi::class.java))
    }

    val tripsRepository: TripsRepository by lazy { RemoteTripsRepository(retrofit.create(TripsApi::class.java)) }

    val emergencyContactsRepository: EmergencyContactsRepository by lazy {
        RemoteEmergencyContactsRepository(
            retrofit.create(EmergencyContactsApi::class.java),
            DataStoreContactsCache(appContext.contactsDataStore, ApiClient.json),
        )
    }

    val authRepository: AuthRepository by lazy { RemoteAuthRepository(authApi, meApi, session, appScope) }

    /**
     * Called once when the app starts: reads the saved session, then loads the account
     * behind it. Offline, the session stays and the account loads on the next request.
     */
    fun start() {
        appScope.launch {
            session.restore()
            if (session.state.value is SessionState.SignedIn) authRepository.refreshAccount()
        }
        // The emergency contacts follow the session: loaded (copy first, then the server)
        // when someone is signed in, removed from the phone when they sign out.
        appScope.launch {
            session.state
                .filter { it !is SessionState.Restoring }
                .map { it is SessionState.SignedIn }
                .distinctUntilChanged()
                .collect { signedIn ->
                    if (signedIn) emergencyContactsRepository.sync() else emergencyContactsRepository.clearLocal()
                }
        }
    }
}
