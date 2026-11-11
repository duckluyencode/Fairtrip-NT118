package com.tripfair.app.data.contacts

import com.tripfair.app.data.network.ApiClient
import com.tripfair.app.data.network.ApiError
import com.tripfair.app.data.network.ApiResult
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private fun contact(id: Int, receivesSos: Boolean = true) =
    """{"id":$id,"name":"Contact $id","relation":"Gia đình","phone":"+8490512884$id","receivesSos":$receivesSos,
       "createdAt":"2026-11-08T02:00:00.000Z","updatedAt":"2026-11-08T02:00:00.000Z"}"""

class InMemoryContactsCache(var saved: List<EmergencyContact> = emptyList()) : ContactsCache {
    override suspend fun load() = saved

    override suspend fun save(contacts: List<EmergencyContact>) {
        saved = contacts
    }

    override suspend fun clear() {
        saved = emptyList()
    }
}

class RemoteEmergencyContactsRepositoryTest {
    private lateinit var server: MockWebServer
    private val cache = InMemoryContactsCache()
    private lateinit var repository: RemoteEmergencyContactsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = ApiClient.retrofit(server.url("/api/v1/").toString(), ApiClient.httpClient())
            .create(EmergencyContactsApi::class.java)
        repository = RemoteEmergencyContactsRepository(api, cache)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(code: Int, body: String = "") {
        server.enqueue(MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build())
    }

    @Test
    fun `offline, sync keeps the copy on the phone`() = runTest {
        cache.saved = listOf(EmergencyContact(7, "Mẹ", phone = "+84905128846"))
        server.close()

        val result = repository.sync()

        assertEquals(ApiError.NETWORK, (result as ApiResult.Failure).error.code)
        assertEquals(listOf(7), repository.contacts.value.map { it.id })
    }

    @Test
    fun `sync replaces the copy with the server's list`() = runTest {
        cache.saved = listOf(EmergencyContact(7, "Mẹ", phone = "+84905128846"))
        respond(200, """{"items":[${contact(1)},${contact(2)}],"limit":5}""")

        repository.sync()

        assertEquals(listOf(1, 2), repository.contacts.value.map { it.id })
        assertEquals(listOf(1, 2), cache.saved.map { it.id })
    }

    @Test
    fun `adding and editing update the list and the copy`() = runTest {
        respond(200, """{"items":[${contact(1)}],"limit":5}""")
        respond(201, contact(2))
        respond(200, contact(1, receivesSos = false))

        repository.refresh()
        repository.add(ContactInput("Contact 2", "Bạn bè", "0905 128 842", receivesSos = true))
        repository.update(1, ContactInput("Contact 1", "Gia đình", "0905 128 841", receivesSos = false))

        assertEquals(listOf(1 to false, 2 to true), cache.saved.map { it.id to it.receivesSos })
        server.takeRequest()
        server.takeRequest().let {
            assertEquals("POST /api/v1/me/emergency-contacts", "${it.method} ${it.url.encodedPath}")
            assertTrue(it.body!!.utf8().contains("\"phone\":\"0905 128 842\""))
        }
        assertEquals("PATCH /api/v1/me/emergency-contacts/1", server.takeRequest().let { "${it.method} ${it.url.encodedPath}" })
    }

    @Test
    fun `saving receivers sends only the changed contacts`() = runTest {
        respond(200, """{"items":[${contact(1)},${contact(2, receivesSos = false)},${contact(3)}],"limit":5}""")
        respond(200, contact(2, receivesSos = true))
        repository.refresh()

        val result = repository.setReceivers(mapOf(1 to true, 2 to true))

        assertTrue(result is ApiResult.Success)
        server.takeRequest()
        server.takeRequest().let {
            assertEquals("/api/v1/me/emergency-contacts/2", it.url.encodedPath)
            assertEquals("""{"receivesSos":true}""", it.body!!.utf8())
        }
        assertEquals(2, server.requestCount)
        assertTrue(repository.contacts.value.all { it.receivesSos })
    }

    @Test
    fun `a refused contact keeps its error code and leaves the list as it was`() = runTest {
        respond(409, """{"statusCode":409,"code":"CONTACT_LIMIT_REACHED","message":"At most 5"}""")

        val result = repository.add(ContactInput("Lan", null, "0905128846", receivesSos = true))

        assertEquals("CONTACT_LIMIT_REACHED", (result as ApiResult.Failure).error.code)
        assertTrue(repository.contacts.value.isEmpty())
    }

    @Test
    fun `signing out clears the copy`() = runTest {
        respond(200, """{"items":[${contact(1)}],"limit":5}""")
        repository.sync()

        repository.clearLocal()

        assertTrue(repository.contacts.value.isEmpty())
        assertTrue(cache.saved.isEmpty())
    }

    @Test
    fun `Vietnamese numbers show the local way`() {
        assertEquals("090 512 8846", displayPhone("+84905128846"))
        assertEquals("+6581234567", displayPhone("+6581234567"))
    }
}
