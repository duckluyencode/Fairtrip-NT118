package com.tripfair.app.data.trips

import com.tripfair.app.data.network.ApiClient
import com.tripfair.app.data.network.ApiResult
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val DETAIL = """{"id":7,"type":"SOLO","name":null,"city":"Đà Nẵng","startDate":"2026-12-12","endDate":"2026-12-14",
  "days":3,"status":"DRAFT","memberCount":1,"myRole":"OWNER","dayStart":"08:00","dayEnd":"21:00","gapMinutes":30,
  "lodgingMode":"HOTEL","inviteCode":null,"voteDeadline":null,
  "members":[{"memberId":3,"fullName":"Duy Đức","role":"OWNER","isMe":true,"interests":[],"budget":2500000,
  "spent":null,"satisfaction":null,"ballotSent":false}],"lodging":null}"""

private const val VOTES = """{"items":[],"memberCount":4,"ballotsSent":1,"mySentAt":null}"""

class RemoteTripsRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteTripsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = ApiClient.retrofit(server.url("/api/v1/").toString(), ApiClient.httpClient()).create(TripsApi::class.java)
        repository = RemoteTripsRepository(api)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(code: Int, body: String = "") {
        server.enqueue(MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build())
    }

    @Test
    fun `a solo draft is sent without the fields it leaves out`() = runTest {
        respond(201, DETAIL)
        val draft = TripDraft("Đà Nẵng", "2026-12-12", "2026-12-14", days = 3, budget = 2_500_000, hotel = true)

        val trip = (repository.create(draft.request(group = false)) as ApiResult.Success).value

        assertEquals(2_500_000L, trip.me?.budget)
        assertEquals(
            """{"type":"SOLO","city":"Đà Nẵng","startDate":"2026-12-12","endDate":"2026-12-14","budget":2500000,"interests":[],"lodging":"HOTEL"}""",
            server.takeRequest().body!!.utf8(),
        )
    }

    @Test
    fun `no hotel for a one-day trip or a 0đ solo budget`() {
        val oneDay = TripDraft("Huế", "2026-12-12", "2026-12-12", days = 1, budget = 900_000, hotel = true)
        val free = TripDraft("Huế", "2026-12-12", "2026-12-13", days = 2, budget = 0, hotel = true)

        assertEquals("SELF", oneDay.request(group = false).lodging)
        assertEquals("SELF", free.request(group = false).lodging)
        // Other members bring their own budgets, so a group keeps asking for hotels.
        assertEquals("HOTEL", free.request(group = true, name = "Nhóm Huế").lodging)
    }

    @Test
    fun `joining sends the code in capitals`() = runTest {
        respond(200, DETAIL)
        repository.join(" dn1214 ", 1_000_000, listOf(Interest.FOOD))
        assertEquals("""{"inviteCode":"DN1214","budget":1000000,"interests":["FOOD"]}""", server.takeRequest().body!!.utf8())
    }

    @Test
    fun `lodging, votes and the ballot reach their routes`() = runTest {
        respond(200, DETAIL)
        respond(200, DETAIL)
        respond(200, VOTES)
        respond(200, VOTES)
        respond(200, VOTES)

        repository.chooseLodging(7, placeId = null, guestsPerRoom = null)
        repository.chooseLodging(7, placeId = 41, guestsPerRoom = 2)
        repository.setVote(7, optionId = 5, voted = true)
        repository.setVote(7, optionId = 5, voted = false)
        repository.sendBallot(7)

        assertEquals("""{"mode":"SELF"}""", server.takeRequest().body!!.utf8())
        assertEquals("""{"mode":"HOTEL","placeId":41,"guestsPerRoom":2}""", server.takeRequest().body!!.utf8())
        assertEquals("PUT /api/v1/trips/7/options/5/vote", server.takeRequest().let { "${it.method} ${it.url.encodedPath}" })
        assertEquals("DELETE /api/v1/trips/7/options/5/vote", server.takeRequest().let { "${it.method} ${it.url.encodedPath}" })
        assertEquals("POST /api/v1/trips/7/ballot", server.takeRequest().let { "${it.method} ${it.url.encodedPath}" })
    }

    @Test
    fun `an itinerary carries each stop's cost and the running total`() = runTest {
        respond(
            200,
            """{"tripId":7,"status":"PLANNED","budgetIncludes":["TICKETS","FOOD","LODGING"],"lodging":null,
               "days":[{"day":1,"date":"2026-12-12","stops":[{"stopId":1,"position":1,"time":"08:00","travelMinutes":0,
               "visitMinutes":120,"placeId":9,"name":"Bà Nà Hills","area":"Hòa Vang","category":"FUN","location":null,
               "attribution":null,"price":{"perPerson":900000,"source":"PARTNER_API","provider":"Klook","isEstimate":false},
               "myCost":900000,"participants":[],"runningTotal":900000,"remaining":1600000}]}],
               "me":{"budget":2500000,"lodgingShare":0,"spentOnPlaces":900000,"total":900000,"remaining":1600000},
               "members":[],"fairness":null,"warnings":[{"code":"PLACE_CLOSED","day":1,"placeId":9}]}""",
        )

        val itinerary = (repository.itinerary(7) as ApiResult.Success).value

        val stop = itinerary.days.single().stops.single()
        assertEquals(900_000L, stop.myCost)
        assertEquals(1_600_000L, stop.remaining)
        assertTrue(itinerary.warnings.any { it.code == "PLACE_CLOSED" })
    }
}
