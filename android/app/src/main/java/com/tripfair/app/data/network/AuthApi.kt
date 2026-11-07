package com.tripfair.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST

// Requests and responses of backend/src/auth and backend/src/users (field names match the API).

@Serializable
data class RegisterRequest(val fullName: String, val contact: String, val password: String)

@Serializable
data class VerifyCodeRequest(val contact: String, val code: String)

/** [purpose] is `REGISTER` or `RESET_PASSWORD`. */
@Serializable
data class ResendCodeRequest(val contact: String, val purpose: String)

@Serializable
data class LoginRequest(val contact: String, val password: String)

@Serializable
data class ContactRequest(val contact: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class ResetPasswordRequest(val resetToken: String, val password: String)

@Serializable
data class UpdateProfileRequest(val fullName: String)

@Serializable
data class DeleteAccountRequest(val password: String)

/** Where a code went: [channel] is `EMAIL` or `SMS`, [sentTo] the normalized contact. */
@Serializable
data class CodeSentResponse(
    val channel: String,
    val sentTo: String,
    val expiresInSeconds: Int,
    val resendInSeconds: Int,
)

@Serializable
data class UserResponse(
    val id: Int,
    val fullName: String,
    val email: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class TokensResponse(val accessToken: String, val refreshToken: String, val expiresIn: Int)

@Serializable
data class SessionResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val user: UserResponse,
)

@Serializable
data class ResetTokenResponse(val resetToken: String, val expiresInSeconds: Int)

/** Routes under `auth/`: no access token needed. */
interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): CodeSentResponse

    @POST("auth/register/verify")
    suspend fun verifyRegistration(@Body body: VerifyCodeRequest): SessionResponse

    @POST("auth/code/resend")
    suspend fun resendCode(@Body body: ResendCodeRequest): CodeSentResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): SessionResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokensResponse

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest)

    @POST("auth/password/forgot")
    suspend fun forgotPassword(@Body body: ContactRequest): CodeSentResponse

    @POST("auth/password/verify")
    suspend fun verifyPasswordReset(@Body body: VerifyCodeRequest): ResetTokenResponse

    @POST("auth/password/reset")
    suspend fun resetPassword(@Body body: ResetPasswordRequest)
}

/** `/me`: the signed-in user's own account (needs the access token). */
interface MeApi {
    @GET("me")
    suspend fun profile(): UserResponse

    @PATCH("me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): UserResponse

    /** Deletes the account; the password is asked again. DELETE with a body needs @HTTP. */
    @HTTP(method = "DELETE", path = "me", hasBody = true)
    suspend fun deleteAccount(@Body body: DeleteAccountRequest)
}
