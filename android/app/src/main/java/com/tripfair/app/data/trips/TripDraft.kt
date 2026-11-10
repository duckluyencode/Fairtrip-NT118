package com.tripfair.app.data.trips

import kotlinx.serialization.Serializable

/**
 * What "Tạo chuyến đi" collects. A solo trip is created from it right away; a group trip
 * first goes to "Tạo nhóm" for its name.
 */
@Serializable
data class TripDraft(
    val city: String,
    /** YYYY-MM-DD */
    val startDate: String,
    val endDate: String,
    val days: Int,
    /** Your own budget in VND: tickets, food and lodging, not transport. */
    val budget: Long,
    /** "Gợi ý chỗ ở qua Agoda" was on. */
    val hotel: Boolean,
) {
    /**
     * A one-day trip has no night to book, and a 0đ solo budget cannot pay for a room,
     * so both are "Tự lo chỗ ở".
     */
    fun wantsHotel(group: Boolean): Boolean = hotel && days > 1 && (group || budget > 0)

    fun request(group: Boolean, name: String? = null) = CreateTripRequest(
        type = if (group) "GROUP" else "SOLO",
        name = name,
        city = city,
        startDate = startDate,
        endDate = endDate,
        budget = budget,
        // Not asked when creating; group members set theirs with their budget.
        interests = emptyList(),
        lodging = if (wantsHotel(group)) "HOTEL" else "SELF",
    )
}
