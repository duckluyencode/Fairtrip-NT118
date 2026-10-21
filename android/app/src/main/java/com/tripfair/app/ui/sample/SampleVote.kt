package com.tripfair.app.ui.sample

import androidx.annotation.DrawableRes
import com.tripfair.app.R

/**
 * A place in the group vote. Vote counts are the illustrative numbers from the Figma frames:
 * [agreed] for the leader's list, [ballotOpen] / [ballotSent] for the member ballot before
 * and after sending.
 */
data class VoteOption(
    val placeId: String,
    val name: String,
    /** Price with unit and source, e.g. "900.000đ / người · vé chính thức". */
    val priceLine: String,
    @param:DrawableRes val image: Int,
    val agreed: String,
    val agreedFraction: Float,
    val ballotOpen: String,
    val ballotSent: String,
)

object SampleVote {
    const val DEADLINE = "20:00"
    const val MEMBERS_LINE = "12–14 tháng 10 · Đức, An, Linh, Khoa"
    const val MEMBER_COUNT = 4
    const val SENT_BEFORE_YOU = 2
    const val SENT_AFTER_YOU = 3

    val options = listOf(
        VoteOption(
            placeId = SamplePlaces.goldenBridge.id,
            name = "Cầu Vàng",
            priceLine = "900.000đ / người · vé chính thức",
            image = R.drawable.sample_vote_golden_bridge,
            agreed = "3/4",
            agreedFraction = 0.75f,
            ballotOpen = "1/2",
            ballotSent = "3/4",
        ),
        VoteOption(
            placeId = SamplePlaces.myKhe.id,
            name = "Bãi biển Mỹ Khê",
            priceLine = "Miễn phí",
            image = R.drawable.sample_vote_my_khe,
            agreed = "4/4",
            agreedFraction = 1f,
            ballotOpen = "2/2",
            ballotSent = "4/4",
        ),
        VoteOption(
            placeId = SamplePlaces.nightMarket.id,
            name = "Chợ đêm Sơn Trà",
            priceLine = "~250.000đ / người · ước tính",
            image = R.drawable.sample_vote_night_market,
            agreed = "4/4",
            agreedFraction = 1f,
            ballotOpen = "2/2",
            ballotSent = "4/4",
        ),
    )

    /** Places the user has ticked before sending (as in Figma). */
    val yourPicks = setOf(SamplePlaces.myKhe.id, SamplePlaces.nightMarket.id)
}
