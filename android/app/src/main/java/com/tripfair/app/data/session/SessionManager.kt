package com.tripfair.app.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The two tokens the backend gives at sign-in (access: 15 minutes, refresh: 30 days). */
data class SessionTokens(val accessToken: String, val refreshToken: String)

sealed interface SessionState {
    /** Reading the saved session at app start; lasts a few milliseconds. */
    data object Restoring : SessionState

    data object SignedOut : SessionState

    data class SignedIn(val tokens: SessionTokens) : SessionState
}

/** Where the session is kept between app launches. */
interface SessionStore {
    /** The saved tokens, or null when there are none or they cannot be read any more. */
    suspend fun load(): SessionTokens?

    suspend fun save(tokens: SessionTokens)

    suspend fun clear()
}

/**
 * The signed-in session, shared by the whole app: the HTTP client reads the access token
 * from here, and the navigation watches [state] to show the login screen when the session
 * ends (sign out, account deleted, or a refresh token that is no longer valid).
 */
class SessionManager(private val store: SessionStore) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Restoring)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    /** The current tokens, or null when signed out. Safe to read from any thread. */
    val tokens: SessionTokens?
        get() = (_state.value as? SessionState.SignedIn)?.tokens

    /** Called once at app start. */
    suspend fun restore() {
        _state.value = store.load()?.let { SessionState.SignedIn(it) } ?: SessionState.SignedOut
    }

    /** After sign-in, and each time the tokens are refreshed (the refresh token rotates). */
    suspend fun signIn(tokens: SessionTokens) {
        store.save(tokens)
        _state.value = SessionState.SignedIn(tokens)
    }

    suspend fun signOut() {
        store.clear()
        _state.value = SessionState.SignedOut
    }
}
