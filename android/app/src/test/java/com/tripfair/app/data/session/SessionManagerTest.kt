package com.tripfair.app.data.session

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Keeps the session in memory, standing in for the encrypted DataStore in tests. */
class InMemorySessionStore(var saved: SessionTokens? = null) : SessionStore {
    override suspend fun load(): SessionTokens? = saved

    override suspend fun save(tokens: SessionTokens) {
        saved = tokens
    }

    override suspend fun clear() {
        saved = null
    }
}

class SessionManagerTest {
    private val tokens = SessionTokens("access", "refresh")

    @Test
    fun `starts restoring, then follows what was saved`() = runTest {
        val manager = SessionManager(InMemorySessionStore(saved = tokens))
        assertEquals(SessionState.Restoring, manager.state.value)
        assertNull(manager.tokens)

        manager.restore()

        assertEquals(SessionState.SignedIn(tokens), manager.state.value)
        assertEquals(tokens, manager.tokens)
    }

    @Test
    fun `nothing saved means signed out`() = runTest {
        val manager = SessionManager(InMemorySessionStore())
        manager.restore()
        assertEquals(SessionState.SignedOut, manager.state.value)
    }

    @Test
    fun `signing in saves the tokens and signing out removes them`() = runTest {
        val store = InMemorySessionStore()
        val manager = SessionManager(store)

        manager.signIn(tokens)
        assertEquals(tokens, store.saved)

        manager.signOut()
        assertNull(store.saved)
        assertEquals(SessionState.SignedOut, manager.state.value)
    }
}
