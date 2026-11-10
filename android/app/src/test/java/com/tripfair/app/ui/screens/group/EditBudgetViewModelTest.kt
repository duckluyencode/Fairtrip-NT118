package com.tripfair.app.ui.screens.group

import com.tripfair.app.R
import com.tripfair.app.data.network.ApiError
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.trips.Interest
import com.tripfair.app.data.trips.TripDetailDto
import com.tripfair.app.data.trips.TripMemberDto
import com.tripfair.app.ui.screens.trip.FakeTrips
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class GroupTrips : FakeTrips() {
    var refusal: String? = null
    val joined = mutableListOf<Triple<String, Long, List<String>>>()
    val updated = mutableListOf<Pair<Long?, List<String>?>>()

    private val trip = TripDetailDto(
        id = 9, type = "GROUP", name = "Nhóm Huế", city = "Huế", startDate = "2026-12-12", endDate = "2026-12-13", days = 2,
        status = "DRAFT", memberCount = 2, myRole = "MEMBER", lodgingMode = "SELF",
        members = listOf(TripMemberDto(4, "Lan", "MEMBER", isMe = true, interests = listOf(Interest.FOOD), budget = 800_000)),
    )

    private fun answer(): ApiResult<TripDetailDto> = refusal?.let { ApiResult.Failure(ApiError(it)) } ?: ApiResult.Success(trip)

    override suspend fun detail(tripId: Int) = answer()

    override suspend fun join(inviteCode: String, budget: Long, interests: List<String>): ApiResult<TripDetailDto> {
        joined += Triple(inviteCode, budget, interests)
        return answer()
    }

    override suspend fun updateMe(tripId: Int, budget: Long?, interests: List<String>?): ApiResult<TripDetailDto> {
        updated += budget to interests
        return answer()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EditBudgetViewModelTest {
    private val trips = GroupTrips()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `editing starts from what the member set and saves the changes`() {
        val viewModel = EditBudgetViewModel(trips)
        viewModel.startEdit(9)
        assertEquals("800000", viewModel.budget)
        assertEquals(setOf(Interest.FOOD), viewModel.interests)
        assertEquals("Nhóm Huế", viewModel.groupName)

        viewModel.onBudgetChange("1200000")
        viewModel.toggle(Interest.BEACH)
        viewModel.toggle(Interest.FOOD)
        viewModel.save()

        assertEquals(listOf<Pair<Long?, List<String>?>>(1_200_000L to listOf(Interest.BEACH)), trips.updated)
        assertEquals(9, viewModel.savedTripId)
    }

    @Test
    fun `joining sends the code with the budget, 0đ when left empty`() {
        val viewModel = EditBudgetViewModel(trips)
        viewModel.startJoin("DN1214")

        viewModel.save()

        assertEquals(listOf(Triple("DN1214", 0L, emptyList<String>())), trips.joined)
        assertEquals(9, viewModel.savedTripId)
    }

    @Test
    fun `a wrong code stays on the screen with the reason`() {
        trips.refusal = "INVITE_CODE_NOT_FOUND"
        val viewModel = EditBudgetViewModel(trips)
        viewModel.startJoin("XXXXXX")

        viewModel.save()

        assertNull(viewModel.savedTripId)
        assertEquals(R.string.trip_error_code_not_found, viewModel.error)
    }
}
