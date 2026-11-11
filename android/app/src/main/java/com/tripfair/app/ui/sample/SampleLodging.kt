package com.tripfair.app.ui.sample

import com.tripfair.app.data.places.LocationDto
import com.tripfair.app.data.trips.ItineraryDayDto
import com.tripfair.app.data.trips.ItineraryDto
import com.tripfair.app.data.trips.ItineraryStopDto
import com.tripfair.app.data.trips.LodgingDto
import com.tripfair.app.data.trips.LodgingOptionDto
import com.tripfair.app.data.trips.MemberCostDto
import com.tripfair.app.data.trips.MyCostsDto
import com.tripfair.app.data.trips.StopParticipantDto
import com.tripfair.app.data.trips.StopPriceDto
import com.tripfair.app.data.trips.TripDetailDto
import com.tripfair.app.data.trips.TripMemberDto

/** Hotels as the backend suggests them, for previews. Prices are per room and night. */
object SampleLodging {
    private fun option(tag: String, id: Int, name: String, rating: Double, km: Double, price: Long) = LodgingOptionDto(
        tag = tag,
        placeId = id,
        name = name,
        rating = rating,
        distanceKm = km,
        pricePerNight = price,
        priceSource = "ESTIMATED",
        provider = "TripFair",
        isEstimate = true,
        nights = 2,
        rooms = 2,
        guestsPerRoom = 2,
        myShare = price,
        leftForPlaces = 2_500_000 - price,
        attribution = "Google Maps",
    )

    val options = listOf(
        option("BALANCED", 41, "Khách sạn Biển Xanh", 4.3, 1.2, 600_000),
        option("CHEAPEST", 42, "Nhà nghỉ Sông Hàn", 3.9, 3.4, 400_000),
        option("NEAREST", 43, "Mỹ Khê View Hotel", 4.5, 0.3, 900_000),
    )
}

/** A planned group trip and its itinerary, for previews of the result and itinerary screens. */
object SampleTripPlan {
    private fun member(id: Int, name: String, me: Boolean = false) =
        TripMemberDto(id, name, if (me) "OWNER" else "MEMBER", isMe = me, budget = if (me) 2_500_000 else null)

    val trip = TripDetailDto(
        id = 1, type = "GROUP", name = "Nhóm Đà Nẵng", city = "Đà Nẵng", startDate = "2026-12-12", endDate = "2026-12-14",
        days = 3, status = "PLANNED", memberCount = 3, myRole = "OWNER", lodgingMode = "HOTEL", inviteCode = "DN1214",
        members = listOf(member(1, "Duy Đức", me = true), member(2, "An"), member(3, "Linh")),
    )

    private val everyone = trip.members.map { StopParticipantDto(it.memberId, it.fullName) }

    private fun stop(id: Int, time: String, name: String, category: String, minutes: Int, price: Long, source: String, provider: String, lat: Double, lng: Double, running: Long) =
        ItineraryStopDto(
            stopId = id, position = id, time = time, visitMinutes = minutes, placeId = 100 + id, name = name, category = category,
            location = LocationDto(lat, lng), price = StopPriceDto(price, source, provider, isEstimate = source != "PARTNER_API"),
            myCost = price, participants = everyone, runningTotal = running, remaining = 2_500_000 - running,
        )

    val itinerary = ItineraryDto(
        tripId = 1,
        status = "PLANNED",
        lodging = LodgingDto(
            placeId = 41, name = "Khách sạn Biển Xanh", checkIn = "2026-12-12", checkOut = "2026-12-14", nights = 2, rooms = 2,
            guestsPerRoom = 2, pricePerNight = 600_000, priceSource = "ESTIMATED", provider = "TripFair", isEstimate = true, myShare = 600_000,
        ),
        days = listOf(
            ItineraryDayDto(
                day = 1,
                date = "2026-12-12",
                stops = listOf(
                    stop(1, "08:00", "Mì Quảng Bà Mua", "FOOD", 45, 50_000, "GOOGLE_API", "Google", 16.0678, 108.2208, 650_000),
                    stop(2, "09:30", "Bãi biển Mỹ Khê", "SCENIC", 120, 0, "ESTIMATED", "TripFair", 16.0544, 108.2469, 650_000),
                    stop(3, "14:00", "Bà Nà Hills", "FUN", 240, 900_000, "PARTNER_API", "Klook", 15.9977, 107.9881, 1_550_000),
                ),
            ),
            ItineraryDayDto(day = 2, date = "2026-12-13", stops = emptyList()),
        ),
        me = MyCostsDto(budget = 2_500_000, lodgingShare = 600_000, spentOnPlaces = 950_000, total = 1_550_000, remaining = 950_000),
        members = listOf(MemberCostDto(1, "Duy Đức", 1_550_000, 92), MemberCostDto(2, "An", 1_550_000, 81), MemberCostDto(3, "Linh", 1_550_000, 95)),
        fairness = 0.99,
    )
}
