package com.tripfair.app.ui.screens.trip

import com.tripfair.app.R
import com.tripfair.app.data.network.ApiError
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.places.LocationDto
import com.tripfair.app.data.trips.DayPlacesDto
import com.tripfair.app.data.trips.ItineraryDto
import com.tripfair.app.data.trips.LodgingDto
import com.tripfair.app.data.trips.LodgingOptionDto
import com.tripfair.app.data.trips.MyCostsDto
import com.tripfair.app.data.trips.TripDetailDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private fun hotel(tag: String, id: Int) = LodgingOptionDto(
    tag = tag, placeId = id, name = "Hotel $id", pricePerNight = 500_000, priceSource = "ESTIMATED", provider = "TripFair",
    isEstimate = true, nights = 2, rooms = 1, guestsPerRoom = 3, myShare = 333_333, leftForPlaces = 1_000_000,
)

private class PlanningTrips : FakeTrips() {
    var hotels: ApiResult<List<LodgingOptionDto>> = ApiResult.Success(listOf(hotel("CHEAPEST", 1), hotel("BALANCED", 2)))
    var planResult: ApiResult<ItineraryDto> = ApiResult.Success(
        ItineraryDto(7, "PLANNED", days = emptyList(), me = MyCostsDto(0, 0, 0, 0, 0)),
    )
    val chosen = mutableListOf<Pair<Int?, Int?>>()

    var detailResult: ApiResult<TripDetailDto> = ApiResult.Failure(ApiError(ApiError.NETWORK))

    override suspend fun detail(tripId: Int): ApiResult<TripDetailDto> = detailResult

    override suspend fun lodgingOptions(tripId: Int, guestsPerRoom: Int?) = hotels

    override suspend fun chooseLodging(tripId: Int, placeId: Int?, guestsPerRoom: Int?): ApiResult<TripDetailDto> {
        chosen += placeId to guestsPerRoom
        return ApiResult.Success(TripDetailDto(7, "SOLO", null, "Huế", "2026-12-12", "2026-12-13", 2, "DRAFT", myRole = "OWNER", lodgingMode = "HOTEL"))
    }

    override suspend fun plan(tripId: Int) = planResult

    var designed: List<DayPlacesDto> = emptyList()

    override suspend fun designItinerary(tripId: Int, days: List<DayPlacesDto>): ApiResult<ItineraryDto> {
        designed = days
        return planResult
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TripPlanViewModelsTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val trips = PlanningTrips()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the balanced hotel is picked first and sent with its room size`() {
        val viewModel = LodgingViewModel(trips)
        viewModel.start(7)
        assertEquals(2, viewModel.selected)

        viewModel.confirm()

        assertEquals(listOf<Pair<Int?, Int?>>(2 to 3), trips.chosen)
        assertTrue(viewModel.done)
    }

    @Test
    fun `the hotel already chosen for the trip stays selected`() {
        val lodging = LodgingDto(
            placeId = 1, checkIn = "2026-12-12", checkOut = "2026-12-13", nights = 1, rooms = 1, guestsPerRoom = 3,
            pricePerNight = 500_000, priceSource = "ESTIMATED", provider = "TripFair", isEstimate = true, myShare = 166_667,
        )
        trips.detailResult = ApiResult.Success(
            TripDetailDto(7, "GROUP", null, "Huế", "2026-12-12", "2026-12-13", 2, "DRAFT", myRole = "OWNER", lodgingMode = "HOTEL", lodging = lodging),
        )
        val viewModel = LodgingViewModel(trips)
        viewModel.start(7)

        assertEquals(1, viewModel.selected)
    }

    @Test
    fun `a one-day trip offers only self-arranged lodging, without an error`() {
        trips.hotels = ApiResult.Failure(ApiError("LODGING_NOT_NEEDED"))
        val viewModel = LodgingViewModel(trips)
        viewModel.start(7)

        assertEquals(SELF_LODGING, viewModel.selected)
        assertNull(viewModel.error)

        viewModel.confirm()
        assertEquals(listOf<Pair<Int?, Int?>>(null to null), trips.chosen)
    }

    @Test
    fun `planning finishes the steps, then is done`() = runTest(dispatcher) {
        val viewModel = OptimizingViewModel(trips)
        viewModel.start(7)
        advanceUntilIdle()

        assertEquals(PLANNING_STEPS, viewModel.step)
        assertTrue(viewModel.done)
    }

    @Test
    fun `planning without a chosen hotel says so and can be retried`() = runTest(dispatcher) {
        trips.planResult = ApiResult.Failure(ApiError("LODGING_NOT_CHOSEN"))
        val viewModel = OptimizingViewModel(trips)
        viewModel.start(7)
        advanceUntilIdle()
        assertEquals("LODGING_NOT_CHOSEN", viewModel.error?.code)
        assertEquals(R.string.trip_error_lodging_not_chosen, tripErrorMessage(viewModel.error!!))

        trips.planResult = ApiResult.Success(ItineraryDto(7, "PLANNED", days = emptyList(), me = MyCostsDto(0, 0, 0, 0, 0)))
        viewModel.plan()
        advanceUntilIdle()

        assertNull(viewModel.error)
        assertTrue(viewModel.done)
    }

    @Test
    fun `a self-designed itinerary sends each day's places in order, without the room`() {
        fun stop(id: String, placeId: String?, kind: StopKind = StopKind.Place) =
            ItineraryStop(id = id, time = "08:00", name = id, detail = "", cost = 0, kind = kind, placeId = placeId)
        val days = listOf(
            ItineraryDay(1, "12/12", "", "", listOf(stop("room", "41", StopKind.Lodging), stop("b", "102"), stop("a", "101")), emptyList()),
            ItineraryDay(2, "13/12", "", "", emptyList(), emptyList()),
        )

        val viewModel = ItineraryViewModel(trips)
        viewModel.design(days.toDayPlaces())

        assertEquals(listOf(DayPlacesDto(1, listOf(102, 101)), DayPlacesDto(2, emptyList())), trips.designed)
        assertTrue(viewModel.saved)
    }

    @Test
    fun `a picked place joins the chosen day once, with its price left to the backend`() {
        val days = listOf(
            ItineraryDay(1, "12/12", "", "", listOf(ItineraryStop("a", "08:00", "A", "", 0, placeId = "101")), emptyList()),
            ItineraryDay(2, "13/12", "", "", emptyList(), emptyList()),
        )
        val viewModel = ItineraryViewModel(trips)
        viewModel.startEditing(days)
        viewModel.onAddPlaceTo(1)

        assertTrue(viewModel.addPlace(205, "Chợ Hàn", "pending"))
        assertEquals(false, viewModel.addPlace(101, "A", "pending"))

        val added = viewModel.editorDays!![1].stops.single()
        assertTrue(added.pricePending && added.isNew)
        assertEquals(listOf(DayPlacesDto(1, listOf(101)), DayPlacesDto(2, listOf(205))), viewModel.editorDays!!.toDayPlaces())
    }

    @Test
    fun `stops keep their relative positions inside the margins`() {
        val north = LocationDto(16.10, 108.20)
        val south = LocationDto(16.00, 108.30)

        val markers = markersFor(listOf(north, south), margin = 0.1f)

        assertEquals(0.1f, markers[0].x, 0.001f)
        assertEquals(0.1f, markers[0].y, 0.001f)
        assertEquals(0.9f, markers[1].x, 0.001f)
        assertEquals(0.9f, markers[1].y, 0.001f)
        assertEquals(listOf(0.5f, 0.5f), markersFor(listOf(north)).single().let { listOf(it.x, it.y) })
    }

    @Test
    fun `the distance follows the stops in order`() {
        // Cầu Rồng to Bãi biển Mỹ Khê is about 3 km in a straight line.
        val km = pathKm(listOf(LocationDto(16.0611, 108.2272), LocationDto(16.0544, 108.2469)))
        assertTrue(km in 1.8..2.4)
        assertEquals(0.0, pathKm(emptyList()), 0.0)
    }
}
