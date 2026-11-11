package com.tripfair.app.data.network

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiCallTest {
    private lateinit var server: MockWebServer
    private lateinit var api: AuthApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = ApiClient.retrofit(server.url("/api/v1/").toString(), ApiClient.httpClient())
            .create(AuthApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(code: Int, body: String = "") {
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build(),
        )
    }

    @Test
    fun `a success is parsed, unknown fields are ignored`() = runTest {
        respond(
            202,
            """{"channel":"EMAIL","sentTo":"an@example.com","expiresInSeconds":900,"resendInSeconds":60,"newField":1}""",
        )

        val result = apiCall(ApiClient.json) { api.forgotPassword(ContactRequest("an@example.com")) }

        assertEquals(
            ApiResult.Success(CodeSentResponse("EMAIL", "an@example.com", 900, 60)),
            result,
        )
        assertEquals("/api/v1/auth/password/forgot", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `an error body becomes its stable code`() = runTest {
        respond(401, """{"statusCode":401,"code":"WRONG_CREDENTIALS","message":"Wrong email or password"}""")

        val result = apiCall(ApiClient.json) { api.login(LoginRequest("an@example.com", "wrong")) }

        assertEquals(
            ApiResult.Failure(ApiError("WRONG_CREDENTIALS", 401, "Wrong email or password")),
            result,
        )
    }

    @Test
    fun `a body that is not a TripFair error is unexpected`() = runTest {
        respond(502, "<html>Bad gateway</html>")

        val result = apiCall(ApiClient.json) { api.login(LoginRequest("an@example.com", "x")) }

        assertEquals(ApiResult.Failure(ApiError(ApiError.UNEXPECTED, 502)), result)
    }

    @Test
    fun `an unreachable server is a network error`() = runTest {
        server.close()

        val result = apiCall(ApiClient.json) { api.login(LoginRequest("an@example.com", "x")) }

        assertTrue(result is ApiResult.Failure && result.error.code == ApiError.NETWORK)
    }

    @Test
    fun `an empty 204 response is a success`() = runTest {
        respond(204)

        val result = apiCall(ApiClient.json) { api.logout(RefreshRequest("token")) }

        assertEquals(ApiResult.Success(Unit), result)
    }
}
