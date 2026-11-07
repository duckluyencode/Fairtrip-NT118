package com.tripfair.app.data.session

import com.tripfair.app.data.network.RefreshRequest
import com.tripfair.app.data.network.TokensResponse
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/** Blocking refresh call, used from inside OkHttp where suspending is not possible. */
interface RefreshApi {
    @POST("auth/refresh")
    fun refresh(@Body body: RefreshRequest): Call<TokensResponse>
}

private fun Request.withToken(token: String): Request =
    newBuilder().header("Authorization", "Bearer $token").build()

/** Adds the access token to every request except the sign-in routes under `auth/`. */
class AuthInterceptor(private val session: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = session.tokens?.accessToken
        val isAuthRoute = request.url.encodedPath.contains("/auth/")
        return if (token == null || isAuthRoute || request.header("Authorization") != null) {
            chain.proceed(request)
        } else {
            chain.proceed(request.withToken(token))
        }
    }
}

/**
 * Answers a 401 on an authenticated request: trades the refresh token for new tokens once,
 * then sends the request again. Requests that fail together wait for that single refresh
 * and reuse its result. A 401 without a token (wrong password at login) is left alone. When
 * the backend refuses the refresh token (expired, revoked, or reused), the session ends and
 * the app shows the login screen; a network error keeps the session for the next try.
 */
class TokenAuthenticator(
    private val session: SessionManager,
    private val refreshApi: RefreshApi,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val sent = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
        // The retried request failed too: stop instead of looping.
        if (response.priorResponse?.code == 401) return null
        synchronized(lock) {
            val current = session.tokens ?: return null
            if (current.accessToken != sent) {
                // Another request refreshed the tokens while this one was waiting.
                return response.request.withToken(current.accessToken)
            }
            val refreshed = try {
                refreshApi.refresh(RefreshRequest(current.refreshToken)).execute()
            } catch (e: IOException) {
                return null
            }
            val body = refreshed.body()
            if (refreshed.isSuccessful && body != null) {
                val tokens = SessionTokens(body.accessToken, body.refreshToken)
                runBlocking { session.signIn(tokens) }
                return response.request.withToken(tokens.accessToken)
            }
            if (refreshed.code() == 401) {
                runBlocking { session.signOut() }
            }
            return null
        }
    }
}
