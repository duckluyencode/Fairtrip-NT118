package com.tripfair.app.data

import android.content.Context
import com.tripfair.app.BuildConfig
import com.tripfair.app.data.auth.AuthRepository
import com.tripfair.app.data.auth.RemoteAuthRepository
import com.tripfair.app.data.network.ApiClient
import com.tripfair.app.data.network.AuthApi
import com.tripfair.app.data.network.MeApi
import com.tripfair.app.data.session.AuthInterceptor
import com.tripfair.app.data.session.DataStoreSessionStore
import com.tripfair.app.data.session.KeystoreTokenCipher
import com.tripfair.app.data.session.RefreshApi
import com.tripfair.app.data.session.SessionManager
import com.tripfair.app.data.session.TokenAuthenticator
import com.tripfair.app.data.session.SessionState
import com.tripfair.app.data.session.sessionDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.Retrofit

/**
 * Creates the repositories once for the whole app. Screens get them through their ViewModels.
 * Swapping a sample repository for a backend one only changes this file.
 */
class AppContainer(context: Context) {
    /** Work that outlives any screen (restoring the session, signing out). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val session = SessionManager(
        DataStoreSessionStore(context.applicationContext.sessionDataStore, KeystoreTokenCipher()),
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
    }
}
