package com.tripfair.app.ui.sample

import androidx.annotation.DrawableRes
import com.tripfair.app.R

/** A hotel suggested from Agoda. Prices are per room and night, in dong. */
data class SampleHotel(
    val id: String,
    /** "Cân bằng", "Rẻ nhất" or "Gần lộ trình nhất". */
    val tag: String,
    val name: String,
    /** Name used on the confirm button, e.g. "Biển Xanh". */
    val shortName: String,
    val rating: String,
    val distanceToRoute: String,
    val pricePerNight: Long,
    @param:DrawableRes val image: Int,
)

object SampleLodging {
    const val TRIP_LINE = "Đà Nẵng · 12–14 tháng 10 · 2 đêm · 2 người/phòng"
    const val FETCHED_AT = "09:35"
    const val PEOPLE_PER_ROOM = 2

    val hotels = listOf(
        SampleHotel(
            id = "bien-xanh",
            tag = "Cân bằng",
            name = "Khách sạn Biển Xanh",
            shortName = "Biển Xanh",
            rating = "8,6/10",
            distanceToRoute = "1,2 km",
            pricePerNight = 600_000,
            image = R.drawable.sample_hotel_bien_xanh,
        ),
        SampleHotel(
            id = "song-han",
            tag = "Rẻ nhất",
            name = "Nhà nghỉ Sông Hàn",
            shortName = "Sông Hàn",
            rating = "7,9/10",
            distanceToRoute = "3,4 km",
            pricePerNight = 400_000,
            image = R.drawable.sample_hotel_song_han,
        ),
        SampleHotel(
            id = "my-khe-view",
            tag = "Gần lộ trình nhất",
            name = "Mỹ Khê View Hotel",
            shortName = "Mỹ Khê View",
            rating = "9,0/10",
            distanceToRoute = "0,3 km",
            pricePerNight = 900_000,
            image = R.drawable.sample_hotel_my_khe_view,
        ),
    )

    /** Your share of the room for the whole stay; people sharing a room split it evenly. */
    fun yourShare(hotel: SampleHotel): Long = hotel.pricePerNight * SampleItinerary.NIGHTS / PEOPLE_PER_ROOM
}

/** One member's line in "Chia chi phí & mức hài lòng". */
data class MemberResult(
    val name: String,
    val isYou: Boolean,
    val spent: Long,
    /** Satisfaction in percent. */
    val satisfaction: Int,
)

/** One place in "Chi phí theo điểm · mỗi người". */
data class CostLine(val name: String, val detail: String, val price: String)

/** "Kết quả tối ưu công bằng" from Figma. */
object SampleOptimization {
    const val SATISFACTION = 90
    const val SUMMARY = "6 địa điểm và lưu trú · 3 ngày · cân bằng sở thích của cả 4 người"

    val members = listOf(
        MemberResult(name = "Đức", isYou = true, spent = 1_920_000, satisfaction = 92),
        MemberResult(name = "An", isYou = false, spent = 1_020_000, satisfaction = 81),
        MemberResult(name = "Linh", isYou = false, spent = 1_920_000, satisfaction = 95),
        MemberResult(name = "Khoa", isYou = false, spent = 1_920_000, satisfaction = 90),
    )

    val costs = listOf(
        CostLine("Khách sạn Biển Xanh · 2 đêm", "Cả nhóm · 2 phòng, chia đều · Agoda", "600.000đ"),
        CostLine("Cầu Vàng · Bà Nà Hills", "Đức, Linh, Khoa · vé chính thức", "900.000đ"),
        CostLine("Chợ đêm Sơn Trà", "Cả nhóm · ước tính", "~250.000đ"),
        CostLine("Phố cổ Hội An", "Cả nhóm · vé chính thức", "120.000đ"),
        CostLine("Mì Quảng Bà Mua", "Cả nhóm · Google", "~50.000đ"),
        CostLine("Bãi biển Mỹ Khê, Cầu Rồng & Sơn Trà", "Cả nhóm", "Miễn phí"),
    )
}
