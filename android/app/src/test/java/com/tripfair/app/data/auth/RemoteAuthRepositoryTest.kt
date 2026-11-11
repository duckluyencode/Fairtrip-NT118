package com.tripfair.app.data.auth

import com.tripfair.app.data.network.ApiClient
import com.tripfair.app.data.network.AuthApi
import com.tripfair.app.data.network.MeApi
import com.tripfair.app.data.session.AuthInterceptor
import com.tripfair.app.data.session.InMemorySessionStore
import com.tripfair.app.data.session.SessionManager
import com.tripfair.app.data.session.SessionState
import com.tripfair.app.data.session.SessionTokens
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private const val USER = """{"id":7,"fullName":"Duy Đức","email":"duc@example.com","phone":null}"""
private const val SESSION = """{"accessToken":"a1","refreshToken":"r1","expiresIn":900,"user":$USER}"""
private const val CODE_SENT = """{"channel":"EMAIL","sentTo":"duc@example.com","expiresInSeconds":900,"resendInSeconds":60}"""

class RemoteAuthRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var session: SessionManager
    private lateinit var auth: AuthApi
    private lateinit var me: MeApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        session = SessionManager(InMemorySessionStore())
        val base = server.url("/api/v1/").toString()
        auth = ApiClient.retrofit(base, ApiClient.httpClient()).create(AuthApi::class.java)
        me = ApiClient.retrofit(base, ApiClient.httpClient(listOf(AuthInterceptor(session)))).create(MeApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun repository(scope: TestScope) = RemoteAuthRepository(auth, me, session, scope, clock = { 1_000L })

    private fun respond(code: Int, body: String = "") {
        server.enqueue(
            MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build(),
        )
    }

    private fun error(code: String, status: Int) = respond(status, """{"statusCode":$status,"code":"$code","message":"x"}""")

    @Test
    fun `login stores the session and the account`() = runTest {
        respond(200, SESSION)
        val repo = repository(this)

        assertNull(repo.login(" duc@example.com ", "matkhau123"))

        assertEquals(SessionState.SignedIn(SessionTokens("a1", "r1")), session.state.value)
        assertEquals(Account("Duy Đức", "duc@example.com", null), repo.currentAccount.value)
        assertEquals("""{"contact":"duc@example.com","password":"matkhau123"}""", server.takeRequest().body?.utf8())
    }

    @Test
    fun `backend error codes become AuthError`() = runTest {
        val repo = repository(this)
        error("WRONG_CREDENTIALS", 401)
        assertEquals(AuthError.WrongCredentials, repo.login("duc@example.com", "x"))
        error("TOO_MANY_REQUESTS", 429)
        assertEquals(AuthError.TooManyRequests, repo.login("duc@example.com", "x"))
        error("SOMETHING_NEW", 500)
        assertEquals(AuthError.Unexpected, repo.login("duc@example.com", "x"))
        server.close()
        assertEquals(AuthError.Network, repo.login("duc@example.com", "x"))
    }

    @Test
    fun `registration sends a code, then the code signs in`() = runTest {
        val repo = repository(this)
        respond(202, CODE_SENT)
        assertNull(repo.startRegistration("Duy Đức", "Duc@Example.com", "matkhau123"))
        assertEquals(PendingCode(CodePurpose.Register, "duc@example.com", 1_000L), repo.pendingCode.value)
        server.takeRequest()

        error("WRONG_CODE", 400)
        assertEquals(AuthError.WrongCode, repo.verifyCode("000000"))
        server.takeRequest()

        respond(200, SESSION)
        assertNull(repo.verifyCode("482915"))
        assertEquals("""{"contact":"duc@example.com","code":"482915"}""", server.takeRequest().body?.utf8())
        assertNull(repo.pendingCode.value)
        assertEquals("Duy Đức", repo.currentAccount.value?.fullName)
    }

    @Test
    fun `password reset keeps the code pending until the new password is saved`() = runTest {
        val repo = repository(this)
        respond(202, CODE_SENT)
        respond(200, """{"resetToken":"reset-1","expiresInSeconds":600}""")
        respond(204)

        assertNull(repo.startPasswordReset("duc@example.com"))
        assertNull(repo.verifyCode("123456"))
        assertEquals(CodePurpose.ResetPassword, repo.pendingCode.value?.purpose)
        assertNull(repo.setNewPassword("matkhaumoi1"))

        server.takeRequest()
        server.takeRequest()
        assertEquals("""{"resetToken":"reset-1","password":"matkhaumoi1"}""", server.takeRequest().body?.utf8())
        assertNull(repo.pendingCode.value)
        assertEquals(AuthError.NoPendingCode, repo.setNewPassword("again12345"))
    }

    @Test
    fun `resend asks for the purpose of the pending code`() = runTest {
        val repo = repository(this)
        respond(202, CODE_SENT)
        repo.startPasswordReset("duc@example.com")
        server.takeRequest()

        error("RESEND_TOO_SOON", 429)
        assertEquals(AuthError.ResendTooSoon, repo.resendCode())
        assertEquals(
            """{"contact":"duc@example.com","purpose":"RESET_PASSWORD"}""",
            server.takeRequest().body?.utf8(),
        )
    }

    @Test
    fun `deleting the account checks the password and ends the session`() = runTest {
        val repo = repository(this)
        session.signIn(SessionTokens("a1", "r1"))

        error("WRONG_CREDENTIALS", 403)
        assertEquals(AuthError.WrongCredentials, repo.deleteAccount("wrong"))
        assertEquals(SessionState.SignedIn(SessionTokens("a1", "r1")), session.state.value)

        respond(204)
        assertNull(repo.deleteAccount("matkhau123"))
        assertEquals(SessionState.SignedOut, session.state.value)

        server.takeRequest()
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("Bearer a1", request.headers["Authorization"])
    }

    @Test
    fun `logout ends the session here and revokes the refresh token`() = runTest {
        val repo = repository(this)
        session.signIn(SessionTokens("a1", "r1"))
        respond(204)

        repo.logout()
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, session.state.value)
        assertEquals("""{"refreshToken":"r1"}""", server.takeRequest().body?.utf8())
    }
}
