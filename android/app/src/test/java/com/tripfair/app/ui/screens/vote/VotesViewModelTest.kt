package com.tripfair.app.ui.screens.vote

import com.tripfair.app.R
import com.tripfair.app.data.network.ApiError
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.trips.TripDetailDto
import com.tripfair.app.data.trips.VoteOptionDto
import com.tripfair.app.data.trips.VoteOptionListDto
import com.tripfair.app.ui.screens.trip.FakeTrips
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Keeps the votes of one member in memory, like the backend does for the caller. */
private class VotingTrips : FakeTrips() {
    var refusal: String? = null
    val added = mutableListOf<Pair<Int, Int>>()
    private var list = VoteOptionListDto(
        items = listOf(VoteOptionDto(1, 101, "Cầu Vàng", votes = 1), VoteOptionDto(2, 102, "Mỹ Khê", votes = 2, votedByMe = true)),
        memberCount = 3,
        ballotsSent = 1,
    )

    private fun answer(): ApiResult<VoteOptionListDto> = refusal?.let { ApiResult.Failure(ApiError(it)) } ?: ApiResult.Success(list)

    override suspend fun detail(tripId: Int): ApiResult<TripDetailDto> = ApiResult.Success(
        TripDetailDto(7, "GROUP", "Nhóm Huế", "Huế", "2026-12-12", "2026-12-13", 2, "DRAFT", 3, "OWNER", lodgingMode = "SELF"),
    )

    override suspend fun options(tripId: Int) = answer()

    override suspend fun setVote(tripId: Int, optionId: Int, voted: Boolean): ApiResult<VoteOptionListDto> {
        if (refusal == null) {
            list = list.copy(items = list.items.map { if (it.id == optionId) it.copy(votedByMe = voted, votes = it.votes + if (voted) 1 else -1) else it })
        }
        return answer()
    }

    override suspend fun sendBallot(tripId: Int): ApiResult<VoteOptionListDto> {
        if (refusal == null) list = list.copy(ballotsSent = list.ballotsSent + 1, mySentAt = "2026-12-01T10:00:00.000Z")
        return answer()
    }

    override suspend fun addOption(tripId: Int, placeId: Int): ApiResult<VoteOptionListDto> {
        added += tripId to placeId
        return answer()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class VotesViewModelTest {
    private val trips = VotingTrips()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a tick is saved at once and the counts follow the backend`() {
        val viewModel = VotesViewModel(trips)
        viewModel.start(7)

        viewModel.toggle(viewModel.votes!!.items.first())

        val first = viewModel.votes!!.items.first()
        assertTrue(first.votedByMe)
        assertEquals(2, first.votes)
        assertEquals("Nhóm Huế", viewModel.trip?.name)
    }

    @Test
    fun `sending the ballot marks it sent`() {
        val viewModel = VotesViewModel(trips)
        viewModel.start(7)

        viewModel.send()

        assertNotNull(viewModel.votes?.mySentAt)
        assertEquals(2, viewModel.votes?.ballotsSent)
    }

    @Test
    fun `a refused vote keeps the list and says why`() {
        val viewModel = VotesViewModel(trips)
        viewModel.start(7)
        trips.refusal = "BALLOT_ALREADY_SENT"

        viewModel.toggle(viewModel.votes!!.items.first())

        assertFalse(viewModel.votes!!.items.first().votedByMe)
        assertEquals(R.string.trip_error_ballot_sent, viewModel.error)
    }
}
