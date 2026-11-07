package com.tripfair.app.data.network

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Builds the HTTP client and Retrofit for the TripFair backend. */
object ApiClient {
    /** Unknown fields are ignored, so a newer backend does not break an older app. */
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    /**
     * [interceptors] run on every request (e.g. adding the access token); [authenticator]
     * answers 401 responses (e.g. refreshing the token). Debug builds also log each request
     * line and status, never headers or bodies, so tokens and passwords stay out of the log.
     */
    fun httpClient(
        interceptors: List<Interceptor> = emptyList(),
        authenticator: Authenticator? = null,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            // Planning a trip can take several seconds on the backend (Google calls).
            .readTimeout(30, TimeUnit.SECONDS)
            .apply {
                interceptors.forEach(::addInterceptor)
                debugInterceptors().forEach(::addInterceptor)
                authenticator?.let(::authenticator)
            }
            .build()

    /** [baseUrl] ends with `/api/v1/`, so service paths are written without a leading slash. */
    fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
