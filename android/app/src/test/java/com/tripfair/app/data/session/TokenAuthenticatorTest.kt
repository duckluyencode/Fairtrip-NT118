package com.tripfair.app.data.session

import com.tripfair.app.data.network.ApiClient
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.network.AuthApi
import com.tripfair.app.data.network.LoginRequest
import com.tripfair.app.data.network.MeApi
import com.tripfair.app.data.network.apiCall
import java.util.Collections
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * A tiny backend: GET /me answers 200 only with the current access token, POST /auth/refresh
 * hands out a new pair once per refresh token (like the real one, which rotates them).
 */
private class FakeBackend : Dispatcher() {
    @Volatile var validAccess = "access-1"
    @Volatile var validRefresh = "refresh-1"
    @Volatile var refreshWorks = true
    val refreshCalls = Collections.synchronizedList(mutableListOf<String>())
    val authHeaders = Collections.synchronizedList(mutableListOf<String?>())

    private fun json(code: Int, body: String) =
        MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.url.encodedPath
        return when {
            path.endsWith("/auth/refresh") -> synchronized(this) {
                refreshCalls += path
                if (!refreshWorks) {
                    json(401, """{"statusCode":401,"code":"INVALID_TOKEN","message":"Invalid token"}""")
                } else {
                    val n = refreshCalls.size + 1
                    validAccess = "access-$n"
                    validRefresh = "refresh-$n"
                    json(200, """{"accessToken":"access-$n","refreshToken":"refresh-$n","expiresIn":900}""")
                }
            }
            path.endsWith("/auth/login") ->
                json(401, """{"statusCode":401,"code":"WRONG_CREDENTIALS","message":"Wrong"}""")
            path.endsWith("/me") -> {
                val header = request.headers["Authorization"]
                authHeaders += header
                if (header == "Bearer $validAccess") {
                    json(200, """{"id":1,"fullName":"Duy Đức","email":"duc@example.com"}""")
                } else {
                    json(401, """{"statusCode":401,"code":"UNAUTHORIZED","message":"No"}""")
                }
            }
            else -> json(404, """{"statusCode":404,"code":"NOT_FOUND","message":"No"}""")
        }
    }
}

class TokenAuthenticatorTest {
    private lateinit var server: MockWebServer
    private val backend = FakeBackend()
    private lateinit var session: SessionManager
    private lateinit var me: MeApi
    private lateinit var auth: AuthApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = backend
        server.start()
        session = SessionManager(InMemorySessionStore())
        val base = server.url("/api/v1/").toString()
        val public = ApiClient.retrofit(base, ApiClient.httpClient())
        val client = ApiClient.httpClient(
            interceptors = listOf(AuthInterceptor(session)),
            authenticator = TokenAuthenticator(session, public.create(RefreshApi::class.java)),
        )
        val retrofit = ApiClient.retrofit(base, client)
        me = retrofit.create(MeApi::class.java)
        auth = retrofit.create(AuthApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `adds the access token`() = runTest {
        session.signIn(SessionTokens("access-1", "refresh-1"))

        val result = apiCall(ApiClient.json) { me.profile() }

        assertTrue(result is ApiResult.Success)
        assertEquals(listOf("Bearer access-1"), backend.authHeaders)
        assertEquals(0, backend.refreshCalls.size)
    }

    @Test
    fun `refreshes an expired access token once and retries`() = runTest {
        session.signIn(SessionTokens("expired", "refresh-1"))

        val result = apiCall(ApiClient.json) { me.profile() }

        assertTrue(result is ApiResult.Success)
        assertEquals(1, backend.refreshCalls.size)
        assertEquals(SessionTokens("access-2", "refresh-2"), session.tokens)
    }

    @Test
    fun `requests failing together share one refresh`() = runBlocking {
        session.signIn(SessionTokens("expired", "refresh-1"))

        val results = (1..5).map { async(kotlinx.coroutines.Dispatchers.IO) { apiCall(ApiClient.json) { me.profile() } } }
            .awaitAll()

        assertTrue(results.all { it is ApiResult.Success })
        assertEquals(1, backend.refreshCalls.size)
    }

    @Test
    fun `a refused refresh token signs out`() = runTest {
        backend.refreshWorks = false
        session.signIn(SessionTokens("expired", "revoked"))

        val result = apiCall(ApiClient.json) { me.profile() }

        assertTrue(result is ApiResult.Failure && result.error.status == 401)
        assertEquals(SessionState.SignedOut, session.state.value)
    }

    @Test
    fun `a wrong password at login does not try to refresh`() = runTest {
        session.signIn(SessionTokens("access-1", "refresh-1"))

        val result = apiCall(ApiClient.json) { auth.login(LoginRequest("duc@example.com", "wrong")) }

        assertTrue(result is ApiResult.Failure && result.error.code == "WRONG_CREDENTIALS")
        assertEquals(0, backend.refreshCalls.size)
        assertEquals(SessionTokens("access-1", "refresh-1"), session.tokens)
    }
}
