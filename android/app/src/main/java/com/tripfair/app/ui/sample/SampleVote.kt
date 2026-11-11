package com.tripfair.app.ui.sample

import com.tripfair.app.data.places.PriceDto
import com.tripfair.app.data.trips.VoteOptionDto
import com.tripfair.app.data.trips.VoteOptionListDto

/** A group's vote as the backend returns it, for previews. */
object SampleVote {
    val votes = VoteOptionListDto(
        items = listOf(
            VoteOptionDto(
                id = 1, placeId = 101, name = "Cầu Vàng", category = "SCENIC",
                price = PriceDto(900_000, 900_000, source = "PARTNER_API", provider = "Klook", isEstimate = false),
                votes = 2, votedByMe = false,
            ),
            VoteOptionDto(
                id = 2, placeId = 102, name = "Bãi biển Mỹ Khê", category = "SCENIC",
                price = PriceDto(0, 0, source = "ESTIMATED", provider = "TripFair", isEstimate = true),
                votes = 3, votedByMe = true,
            ),
            VoteOptionDto(
                id = 3, placeId = 103, name = "Chợ đêm Sơn Trà", category = "FOOD",
                price = PriceDto(150_000, 250_000, source = "ESTIMATED", provider = "TripFair", isEstimate = true),
                votes = 3, votedByMe = true, addedByMe = true,
            ),
        ),
        memberCount = 3,
        ballotsSent = 2,
    )
}
