package com.tripfair.app.data.trips

import com.tripfair.app.data.network.ApiClient
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.network.apiCall

/**
 * Solo and group trips, their lodging, votes and itinerary. Every call goes to the backend;
 * screens load what they show each time they open, so a change made by another member
 * appears the next time the screen is opened.
 */
interface TripsRepository {
    suspend fun list(): ApiResult<List<TripSummaryDto>>

    suspend fun create(request: CreateTripRequest): ApiResult<TripDetailDto>

    suspend fun join(inviteCode: String, budget: Long, interests: List<String>): ApiResult<TripDetailDto>

    suspend fun detail(tripId: Int): ApiResult<TripDetailDto>

    /** Your own budget and interests in the trip. */
    suspend fun updateMe(tripId: Int, budget: Long?, interests: List<String>?): ApiResult<TripDetailDto>

    suspend fun renewInviteCode(tripId: Int): ApiResult<TripDetailDto>

    suspend fun lodgingOptions(tripId: Int, guestsPerRoom: Int?): ApiResult<List<LodgingOptionDto>>

    /** A suggested hotel ([placeId]), or "Tự lo chỗ ở" when [placeId] is null. */
    suspend fun chooseLodging(tripId: Int, placeId: Int?, guestsPerRoom: Int?): ApiResult<TripDetailDto>

    suspend fun options(tripId: Int): ApiResult<VoteOptionListDto>

    suspend fun addOption(tripId: Int, placeId: Int): ApiResult<VoteOptionListDto>

    suspend fun setVote(tripId: Int, optionId: Int, voted: Boolean): ApiResult<VoteOptionListDto>

    suspend fun sendBallot(tripId: Int): ApiResult<VoteOptionListDto>

    /** Runs the planner (Greedy, local search, fairness check) and saves the itinerary. */
    suspend fun plan(tripId: Int): ApiResult<ItineraryDto>

    suspend fun itinerary(tripId: Int): ApiResult<ItineraryDto>

    /** "Tự thiết kế lộ trình": the places of each day, in visiting order. */
    suspend fun designItinerary(tripId: Int, days: List<DayPlacesDto>): ApiResult<ItineraryDto>
}

class RemoteTripsRepository(private val api: TripsApi) : TripsRepository {
    private val json = ApiClient.json

    override suspend fun list() = apiCall(json) { api.list() }

    override suspend fun create(request: CreateTripRequest) = apiCall(json) { api.create(request) }

    override suspend fun join(inviteCode: String, budget: Long, interests: List<String>) =
        apiCall(json) { api.join(JoinTripRequest(inviteCode.trim().uppercase(), budget, interests)) }

    override suspend fun detail(tripId: Int) = apiCall(json) { api.detail(tripId) }

    override suspend fun updateMe(tripId: Int, budget: Long?, interests: List<String>?) =
        apiCall(json) { api.updateMe(tripId, MembershipPatch(budget, interests)) }

    override suspend fun renewInviteCode(tripId: Int) = apiCall(json) { api.renewInviteCode(tripId) }

    override suspend fun lodgingOptions(tripId: Int, guestsPerRoom: Int?) =
        apiCall(json) { api.lodgingOptions(tripId, guestsPerRoom) }

    override suspend fun chooseLodging(tripId: Int, placeId: Int?, guestsPerRoom: Int?) = apiCall(json) {
        val request = if (placeId == null) {
            ChooseLodgingRequest(mode = "SELF")
        } else {
            ChooseLodgingRequest(mode = "HOTEL", placeId = placeId, guestsPerRoom = guestsPerRoom)
        }
        api.chooseLodging(tripId, request)
    }

    override suspend fun options(tripId: Int) = apiCall(json) { api.options(tripId) }

    override suspend fun addOption(tripId: Int, placeId: Int) = apiCall(json) { api.addOption(tripId, AddOptionRequest(placeId)) }

    override suspend fun setVote(tripId: Int, optionId: Int, voted: Boolean) = apiCall(json) {
        if (voted) api.vote(tripId, optionId) else api.unvote(tripId, optionId)
    }

    override suspend fun sendBallot(tripId: Int) = apiCall(json) { api.sendBallot(tripId) }

    override suspend fun plan(tripId: Int) = apiCall(json) { api.plan(tripId) }

    override suspend fun itinerary(tripId: Int) = apiCall(json) { api.itinerary(tripId) }

    override suspend fun designItinerary(tripId: Int, days: List<DayPlacesDto>) =
        apiCall(json) { api.designItinerary(tripId, DesignItineraryRequest(days)) }
}
