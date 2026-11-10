package com.tripfair.app.ui.screens.trip

import com.tripfair.app.R
import com.tripfair.app.data.network.ApiError
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.trips.CreateTripRequest
import com.tripfair.app.data.trips.DayPlacesDto
import com.tripfair.app.data.trips.ItineraryDto
import com.tripfair.app.data.trips.LodgingOptionDto
import com.tripfair.app.data.trips.TripDetailDto
import com.tripfair.app.data.trips.TripSummaryDto
import com.tripfair.app.data.trips.TripsRepository
import com.tripfair.app.data.trips.VoteOptionListDto
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Creates trips with id 7 and remembers what was asked; the other calls are not used here. */
open class FakeTrips : TripsRepository {
    val created = mutableListOf<CreateTripRequest>()
    var failure: String? = null

    override suspend fun create(request: CreateTripRequest): ApiResult<TripDetailDto> {
        created += request
        failure?.let { return ApiResult.Failure(ApiError(it)) }
        return ApiResult.Success(
            TripDetailDto(7, request.type, request.name, request.city, request.startDate, request.endDate, 3, "DRAFT", myRole = "OWNER", lodgingMode = request.lodging ?: "SELF"),
        )
    }

    override suspend fun list(): ApiResult<List<TripSummaryDto>> = TODO()
    override suspend fun join(inviteCode: String, budget: Long, interests: List<String>): ApiResult<TripDetailDto> = TODO()
    override suspend fun detail(tripId: Int): ApiResult<TripDetailDto> = TODO()
    override suspend fun updateMe(tripId: Int, budget: Long?, interests: List<String>?): ApiResult<TripDetailDto> = TODO()
    override suspend fun renewInviteCode(tripId: Int): ApiResult<TripDetailDto> = TODO()
    val removed = mutableListOf<String>()

    override suspend fun leave(tripId: Int): ApiResult<Unit> {
        removed += "leave $tripId"
        return ApiResult.Success(Unit)
    }

    override suspend fun delete(tripId: Int): ApiResult<Unit> {
        removed += "delete $tripId"
        return ApiResult.Success(Unit)
    }

    override suspend fun lodgingOptions(tripId: Int, guestsPerRoom: Int?): ApiResult<List<LodgingOptionDto>> = TODO()
    override suspend fun chooseLodging(tripId: Int, placeId: Int?, guestsPerRoom: Int?): ApiResult<TripDetailDto> = TODO()
    override suspend fun options(tripId: Int): ApiResult<VoteOptionListDto> = TODO()
    override suspend fun addOption(tripId: Int, placeId: Int): ApiResult<VoteOptionListDto> = TODO()
    override suspend fun setVote(tripId: Int, optionId: Int, voted: Boolean): ApiResult<VoteOptionListDto> = TODO()
    override suspend fun sendBallot(tripId: Int): ApiResult<VoteOptionListDto> = TODO()
    override suspend fun plan(tripId: Int): ApiResult<ItineraryDto> = TODO()
    override suspend fun itinerary(tripId: Int): ApiResult<ItineraryDto> = TODO()
    override suspend fun designItinerary(tripId: Int, days: List<DayPlacesDto>): ApiResult<ItineraryDto> = TODO()
}

@OptIn(ExperimentalCoroutinesApi::class)
class CreateTripViewModelTest {
    private val trips = FakeTrips()
    private val viewModel by lazy { CreateTripViewModel(trips) }
    private val start = LocalDate.of(2026, 12, 12)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the destination and the dates are required`() {
        viewModel.submit()

        assertEquals(R.string.trip_error_city_missing, viewModel.state.cityError)
        assertEquals(R.string.trip_error_dates_missing, viewModel.state.datesError)
        assertTrue(trips.created.isEmpty())
    }

    @Test
    fun `a trip longer than 15 days is refused before asking the backend`() {
        viewModel.onCityChange("Đà Nẵng")
        viewModel.onDatesChange(start, start.plusDays(15))

        viewModel.submit()

        assertEquals(R.string.trip_error_dates_too_long, viewModel.state.datesError)
    }

    @Test
    fun `a group goes on to naming it without creating anything yet`() {
        viewModel.onCityChange(" Đà Nẵng ")
        viewModel.onDatesChange(start, start.plusDays(2))
        viewModel.onBudgetChange("2500000")

        viewModel.submit()

        val next = viewModel.next as CreateTripNext.NameGroup
        assertEquals("Đà Nẵng", next.draft.city)
        assertEquals(3, next.draft.days)
        assertTrue(trips.created.isEmpty())
    }

    @Test
    fun `a solo trip is created, then goes to lodging or straight to planning`() {
        viewModel.onGroupChange(false)
        viewModel.onCityChange("Đà Nẵng")
        viewModel.onDatesChange(start, start.plusDays(2))
        viewModel.onBudgetChange("2500000")
        viewModel.submit()
        assertEquals(CreateTripNext.ChooseLodging(7), viewModel.next)

        viewModel.onNavigated()
        viewModel.onBudgetChange("")
        viewModel.submit()

        assertEquals(CreateTripNext.Plan(7), viewModel.next)
        assertEquals(listOf("HOTEL", "SELF"), trips.created.map { it.lodging })
        assertEquals(0L, trips.created.last().budget)
    }

    @Test
    fun `a refused trip shows why and stays on the form`() {
        trips.failure = ApiError.NETWORK
        viewModel.onGroupChange(false)
        viewModel.onCityChange("Đà Nẵng")
        viewModel.onDatesChange(start, start)

        viewModel.submit()

        assertNull(viewModel.next)
        assertEquals(R.string.error_network, viewModel.state.error)
    }

    @Test
    fun `home shows the next trip to start, else the latest one`() {
        fun trip(id: Int, start: String, end: String) = TripSummaryDto(id, "SOLO", null, "Huế", start, end, 1, "DRAFT", 1, "OWNER")
        val today = LocalDate.of(2026, 12, 10)
        val past = trip(1, "2026-11-01", "2026-11-02")
        val ongoing = trip(2, "2026-12-09", "2026-12-11")
        val later = trip(3, "2026-12-20", "2026-12-21")

        assertEquals(2, listOf(later, past, ongoing).upcoming(today)?.id)
        assertEquals(1, listOf(past).upcoming(today)?.id)
    }
}
