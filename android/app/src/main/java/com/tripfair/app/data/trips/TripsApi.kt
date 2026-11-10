package com.tripfair.app.data.trips

import com.tripfair.app.data.places.LocationDto
import com.tripfair.app.data.places.PriceDto
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Values of the backend's `Interest` enum, in the order the preference chips show them. */
object Interest {
    const val FOOD = "FOOD"
    const val BEACH = "BEACH"
    const val SCENERY = "SCENERY"
    const val RELAX = "RELAX"
    const val FUN = "FUN"
    const val PHOTO = "PHOTO"
    const val CULTURE = "CULTURE"
    const val EXPLORE = "EXPLORE"

    val all = listOf(FOOD, BEACH, SCENERY, RELAX, FUN, PHOTO, CULTURE, EXPLORE)
}

@Serializable
data class TripSummaryDto(
    val id: Int,
    /** SOLO or GROUP. */
    val type: String,
    val name: String? = null,
    val city: String,
    /** YYYY-MM-DD */
    val startDate: String,
    val endDate: String,
    val days: Int,
    /** DRAFT (no itinerary yet) or PLANNED. */
    val status: String,
    val memberCount: Int = 1,
    /** OWNER or MEMBER. */
    val myRole: String,
) {
    val isGroup: Boolean get() = type == "GROUP"
    val isPlanned: Boolean get() = status == "PLANNED"
}

@Serializable
data class TripMemberDto(
    val memberId: Int,
    val fullName: String,
    val role: String,
    val isMe: Boolean = false,
    val interests: List<String> = emptyList(),
    /** Only for yourself: nobody sees another member's budget. */
    val budget: Long? = null,
    /** Lodging plus places, once the trip is planned; the whole group sees it. */
    val spent: Long? = null,
    val satisfaction: Int? = null,
    val ballotSent: Boolean = false,
) {
    val isOwner: Boolean get() = role == "OWNER"
}

@Serializable
data class LodgingDto(
    val placeId: Int,
    val name: String? = null,
    val address: String? = null,
    val location: LocationDto? = null,
    val checkIn: String,
    val checkOut: String,
    val nights: Int,
    val rooms: Int,
    val guestsPerRoom: Int,
    /** VND for one room and one night. */
    val pricePerNight: Long,
    val priceSource: String,
    val provider: String,
    val isEstimate: Boolean,
    /** Your part of the room cost for the whole stay. */
    val myShare: Long,
    val attribution: String? = null,
)

@Serializable
data class TripDetailDto(
    val id: Int,
    val type: String,
    val name: String? = null,
    val city: String,
    val startDate: String,
    val endDate: String,
    val days: Int,
    val status: String,
    val memberCount: Int = 1,
    val myRole: String,
    val dayStart: String = "08:00",
    val dayEnd: String = "21:00",
    val gapMinutes: Int = 30,
    /** HOTEL or SELF ("Tự lo chỗ ở"). */
    val lodgingMode: String,
    /** Group trips: the code friends type to join. */
    val inviteCode: String? = null,
    /** ISO 8601. */
    val voteDeadline: String? = null,
    val members: List<TripMemberDto> = emptyList(),
    val lodging: LodgingDto? = null,
) {
    val isGroup: Boolean get() = type == "GROUP"
    val isPlanned: Boolean get() = status == "PLANNED"
    val isOwner: Boolean get() = myRole == "OWNER"
    val me: TripMemberDto? get() = members.firstOrNull { it.isMe }
}

@Serializable
data class LodgingOptionDto(
    /** CHEAPEST, BALANCED or NEAREST. */
    val tag: String,
    val placeId: Int,
    val name: String,
    val address: String? = null,
    val location: LocationDto? = null,
    val rating: Double? = null,
    /** km from the middle of the trip's places. */
    val distanceKm: Double? = null,
    val pricePerNight: Long,
    val priceSource: String,
    val provider: String,
    val isEstimate: Boolean,
    val nights: Int,
    val rooms: Int,
    val guestsPerRoom: Int,
    val myShare: Long,
    /** Your budget left for places after the room. */
    val leftForPlaces: Long,
    val attribution: String = "",
)

@Serializable
data class VoteOptionDto(
    val id: Int,
    val placeId: Int,
    val name: String? = null,
    val area: String? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val price: PriceDto? = null,
    val attribution: String? = null,
    val votes: Int = 0,
    val votedByMe: Boolean = false,
    val addedByMe: Boolean = false,
)

@Serializable
data class VoteOptionListDto(
    val items: List<VoteOptionDto>,
    val memberCount: Int,
    val ballotsSent: Int,
    /** When you sent your ballot (ISO 8601); null while you can still change your votes. */
    val mySentAt: String? = null,
)

@Serializable
data class StopPriceDto(
    val perPerson: Long,
    val source: String,
    val provider: String,
    val isEstimate: Boolean,
)

@Serializable
data class StopParticipantDto(val memberId: Int, val fullName: String)

@Serializable
data class ItineraryStopDto(
    val stopId: Int,
    val position: Int,
    /** "HH:mm" */
    val time: String,
    val travelMinutes: Int = 0,
    val visitMinutes: Int,
    val placeId: Int,
    val name: String? = null,
    val area: String? = null,
    val category: String? = null,
    val location: LocationDto? = null,
    val attribution: String? = null,
    val price: StopPriceDto,
    /** What you pay here (0 when an area ticket covers it); null when you skip this stop. */
    val myCost: Long? = null,
    val participants: List<StopParticipantDto> = emptyList(),
    /** Your spending so far, lodging included, after this stop. */
    val runningTotal: Long,
    /** Your budget minus [runningTotal]. */
    val remaining: Long,
)

@Serializable
data class ItineraryDayDto(
    val day: Int,
    /** YYYY-MM-DD */
    val date: String,
    val stops: List<ItineraryStopDto>,
)

@Serializable
data class MyCostsDto(
    val budget: Long,
    val lodgingShare: Long,
    val spentOnPlaces: Long,
    val total: Long,
    val remaining: Long,
)

@Serializable
data class MemberCostDto(
    val memberId: Int,
    val fullName: String,
    val spent: Long,
    val satisfaction: Int? = null,
)

@Serializable
data class ItineraryWarningDto(
    /** DAY_TOO_LONG, PLACE_CLOSED, OVER_BUDGET or MEMBER_OVER_BUDGET. */
    val code: String,
    val day: Int? = null,
    val placeId: Int? = null,
)

@Serializable
data class ItineraryDto(
    val tripId: Int,
    val status: String,
    val lodging: LodgingDto? = null,
    val days: List<ItineraryDayDto>,
    val me: MyCostsDto,
    val members: List<MemberCostDto> = emptyList(),
    /** Jain's fairness index of the members' satisfaction, 0–1. */
    val fairness: Double? = null,
    val warnings: List<ItineraryWarningDto> = emptyList(),
)

@Serializable
data class CreateTripRequest(
    val type: String,
    val name: String? = null,
    val city: String,
    val startDate: String,
    val endDate: String,
    val budget: Long,
    val interests: List<String>,
    val lodging: String? = null,
    val placeIds: List<Int>? = null,
)

@Serializable
data class JoinTripRequest(val inviteCode: String, val budget: Long, val interests: List<String>)

@Serializable
data class MembershipPatch(val budget: Long? = null, val interests: List<String>? = null)

@Serializable
data class ChooseLodgingRequest(val mode: String, val placeId: Int? = null, val guestsPerRoom: Int? = null)

@Serializable
data class AddOptionRequest(val placeId: Int)

@Serializable
data class DayPlacesDto(val day: Int, val placeIds: List<Int>)

@Serializable
data class DesignItineraryRequest(val days: List<DayPlacesDto>)

/** Routes under `trips`, all with the access token. */
interface TripsApi {
    @GET("trips")
    suspend fun list(): List<TripSummaryDto>

    @POST("trips")
    suspend fun create(@Body body: CreateTripRequest): TripDetailDto

    @POST("trips/join")
    suspend fun join(@Body body: JoinTripRequest): TripDetailDto

    @GET("trips/{id}")
    suspend fun detail(@Path("id") id: Int): TripDetailDto

    @PATCH("trips/{id}/me")
    suspend fun updateMe(@Path("id") id: Int, @Body body: MembershipPatch): TripDetailDto

    @DELETE("trips/{id}/me")
    suspend fun leave(@Path("id") id: Int)

    @DELETE("trips/{id}")
    suspend fun delete(@Path("id") id: Int)

    @POST("trips/{id}/invite-code")
    suspend fun renewInviteCode(@Path("id") id: Int): TripDetailDto

    @GET("trips/{id}/lodging/options")
    suspend fun lodgingOptions(@Path("id") id: Int, @Query("guestsPerRoom") guestsPerRoom: Int?): List<LodgingOptionDto>

    @PUT("trips/{id}/lodging")
    suspend fun chooseLodging(@Path("id") id: Int, @Body body: ChooseLodgingRequest): TripDetailDto

    @GET("trips/{id}/options")
    suspend fun options(@Path("id") id: Int): VoteOptionListDto

    @POST("trips/{id}/options")
    suspend fun addOption(@Path("id") id: Int, @Body body: AddOptionRequest): VoteOptionListDto

    @DELETE("trips/{id}/options/{optionId}")
    suspend fun removeOption(@Path("id") id: Int, @Path("optionId") optionId: Int)

    @PUT("trips/{id}/options/{optionId}/vote")
    suspend fun vote(@Path("id") id: Int, @Path("optionId") optionId: Int): VoteOptionListDto

    @DELETE("trips/{id}/options/{optionId}/vote")
    suspend fun unvote(@Path("id") id: Int, @Path("optionId") optionId: Int): VoteOptionListDto

    @POST("trips/{id}/ballot")
    suspend fun sendBallot(@Path("id") id: Int): VoteOptionListDto

    @POST("trips/{id}/plan")
    suspend fun plan(@Path("id") id: Int): ItineraryDto

    @GET("trips/{id}/itinerary")
    suspend fun itinerary(@Path("id") id: Int): ItineraryDto

    @PUT("trips/{id}/itinerary")
    suspend fun designItinerary(@Path("id") id: Int, @Body body: DesignItineraryRequest): ItineraryDto
}
