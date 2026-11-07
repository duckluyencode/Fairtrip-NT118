package com.tripfair.app.data.network

import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** Body of every backend error: `{ statusCode, code, message, details? }`. */
@Serializable
internal data class ApiErrorBody(
    val statusCode: Int = 0,
    val code: String = "",
    val message: String = "",
)

/**
 * Why a request failed. [code] is the backend's stable error code (e.g. `WRONG_CREDENTIALS`),
 * or [NETWORK] when the backend could not be reached. Screens choose their message by code.
 */
data class ApiError(val code: String, val status: Int? = null, val message: String = "") {
    companion object {
        /** No connection, DNS failure or timeout: the request may not have reached the server. */
        const val NETWORK = "NETWORK_UNAVAILABLE"

        /** The server answered with something that is not a TripFair error body. */
        const val UNEXPECTED = "UNEXPECTED_RESPONSE"
    }
}

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

/**
 * Runs one Retrofit call and turns HTTP errors and connection problems into
 * [ApiResult.Failure], so callers never handle Retrofit or OkHttp exceptions themselves.
 * Coroutine cancellation is not caught: leaving a screen still cancels its requests.
 */
suspend fun <T> apiCall(json: Json, block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        ApiResult.Failure(errorOf(json, e.code(), e.response()?.errorBody()?.string()))
    } catch (e: IOException) {
        ApiResult.Failure(ApiError(ApiError.NETWORK, message = e.message.orEmpty()))
    } catch (e: SerializationException) {
        ApiResult.Failure(ApiError(ApiError.UNEXPECTED, message = e.message.orEmpty()))
    }

internal fun errorOf(json: Json, status: Int, body: String?): ApiError {
    val parsed = body?.let {
        try {
            json.decodeFromString(ApiErrorBody.serializer(), it)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    return if (parsed != null && parsed.code.isNotEmpty()) {
        ApiError(parsed.code, status, parsed.message)
    } else {
        ApiError(ApiError.UNEXPECTED, status)
    }
}
