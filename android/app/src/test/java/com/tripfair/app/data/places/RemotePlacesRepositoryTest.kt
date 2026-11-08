package com.tripfair.app.data.places

import com.tripfair.app.data.network.ApiClient
import com.tripfair.app.data.network.ApiResult
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private fun place(id: Int) = """{"id":$id,"source":"GOOGLE","name":"Place $id","category":"SCENIC","isFavorite":false}"""

class RemotePlacesRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemotePlacesRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = ApiClient.retrofit(server.url("/api/v1/").toString(), ApiClient.httpClient()).create(PlacesApi::class.java)
        repository = RemotePlacesRepository(api)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(code: Int, body: String = "") {
        server.enqueue(MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build())
    }

    @Test
    fun `google results page with their token`() = runTest {
        respond(200, """{"items":[${place(1)}],"page":1,"limit":20,"total":null,"nextPageToken":"tok-2"}""")

        val result = repository.search(" cầu rồng ", "SCENIC")

        val page = (result as ApiResult.Success).value
        assertEquals(listOf(1), page.items.map { it.id })
        assertEquals(PageCursor.Token("tok-2"), page.next)
        val url = server.takeRequest().url
        assertEquals("cầu rồng", url.queryParameter("q"))
        assertEquals("SCENIC", url.queryParameter("category"))
        assertNull(url.queryParameter("page"))
    }

    @Test
    fun `catalog results page by number until the total is reached`() = runTest {
        respond(200, """{"items":[${place(1)},${place(2)}],"page":1,"limit":2,"total":3,"nextPageToken":null}""")
        respond(200, """{"items":[${place(3)}],"page":2,"limit":2,"total":3,"nextPageToken":null}""")

        val first = (repository.search("da nang", null) as ApiResult.Success).value
        assertEquals(PageCursor.Number(2), first.next)
        val second = (repository.search("da nang", null, first.next) as ApiResult.Success).value
        assertNull(second.next)

        server.takeRequest()
        assertEquals("2", server.takeRequest().url.queryParameter("page"))
    }

    @Test
    fun `an empty query browses instead of searching`() = runTest {
        respond(200, """{"items":[],"page":1,"limit":20,"total":0}""")
        repository.search("   ", null)
        assertNull(server.takeRequest().url.queryParameter("q"))
    }

    @Test
    fun `history, favorites and their changes reach the right routes`() = runTest {
        respond(200, """{"items":[{"id":5,"query":"Cầu Vàng","category":null,"searchedAt":"2026-11-08T02:00:00.000Z"}]}""")
        respond(204)
        respond(200, """{"items":[${place(9)}]}""")
        respond(204)
        respond(204)

        assertEquals("Cầu Vàng", (repository.history() as ApiResult.Success).value.single().query)
        repository.deleteHistoryEntry(5)
        assertEquals(9, (repository.favorites() as ApiResult.Success).value.single().id)
        repository.setFavorite(9, true)
        repository.setFavorite(9, false)

        assertEquals("/api/v1/me/search-history", server.takeRequest().url.encodedPath)
        server.takeRequest().let { assertEquals("DELETE /api/v1/me/search-history/5", "${it.method} ${it.url.encodedPath}") }
        server.takeRequest()
        server.takeRequest().let { assertEquals("PUT /api/v1/me/favorites/9", "${it.method} ${it.url.encodedPath}") }
        server.takeRequest().let { assertEquals("DELETE /api/v1/me/favorites/9", "${it.method} ${it.url.encodedPath}") }
    }

    @Test
    fun `place detail carries the price and its source`() = runTest {
        respond(
            200,
            """{"id":3,"source":"GOOGLE","name":"Mì Quảng","category":"FOOD","visitMinutes":60,
               "price":{"min":50000,"max":150000,"currency":"VND","source":"ESTIMATED","provider":"TripFair","isEstimate":true,"includedIn":null},
               "openingHours":{"allDay":false,"opensAt":"06:00","closesAt":"21:00","closedToday":false},"attribution":"Google Maps"}""",
        )

        val detail = (repository.detail(3, "session-1") as ApiResult.Success).value

        assertEquals(PriceDto(50_000, 150_000, "VND", "ESTIMATED", "TripFair", true), detail.price)
        assertEquals("session-1", server.takeRequest().url.queryParameter("sessionToken"))
    }
}
