package com.tripfair.app.ui.sample

import com.tripfair.app.ui.screens.trip.ItineraryDay
import com.tripfair.app.ui.screens.trip.ItineraryStop
import com.tripfair.app.ui.screens.trip.MapMarker
import com.tripfair.app.ui.screens.trip.StopKind

/**
 * The optimized 3-day plan from Figma ("Lộ trình theo ngày"). Day 1 is the Figma frame;
 * days 2 and 3 hold the other places of "Kết quả tối ưu" so the totals match (1.920.000đ).
 */
object SampleItinerary {
    const val TITLE = "Đà Nẵng · 3 ngày"
    const val BUDGET = 2_500_000L
    const val NIGHTS = 2

    private val hotelNight1 = ItineraryStop(
        id = "bien-xanh-night-1",
        time = "21:00",
        name = "Khách sạn Biển Xanh",
        detail = "Nhận phòng · đêm 1/2 · Agoda",
        cost = 300_000,
        kind = StopKind.Lodging,
    )

    private val dragonBridge = ItineraryStop(
        id = "dragon-bridge",
        time = "14:00",
        name = "Cầu Rồng & Sơn Trà",
        detail = "Tham quan · 3 giờ",
        cost = 0,
    )

    val days = listOf(
        ItineraryDay(
            number = 1,
            date = "12/10",
            title = "Biển & ẩm thực",
            distance = "18 km",
            stops = listOf(
                ItineraryStop(
                    id = "mi-quang",
                    time = "08:00",
                    name = "Mì Quảng Bà Mua",
                    detail = "Ăn sáng · 45 phút · Google",
                    cost = 50_000,
                    estimated = true,
                    placeId = SamplePlaces.miQuang.id,
                ),
                ItineraryStop(
                    id = "my-khe",
                    time = "09:30",
                    name = "Bãi biển Mỹ Khê",
                    detail = "Tắm biển · 2 giờ",
                    cost = 0,
                    placeId = SamplePlaces.myKhe.id,
                ),
                dragonBridge,
                ItineraryStop(
                    id = "night-market",
                    time = "18:30",
                    name = "Chợ đêm Sơn Trà",
                    detail = "Ăn tối · 2 giờ · ước tính",
                    cost = 250_000,
                    estimated = true,
                    placeId = SamplePlaces.nightMarket.id,
                ),
                hotelNight1,
            ),
            markers = listOf(
                MapMarker(1, 0.21f, 0.49f),
                MapMarker(2, 0.48f, 0.68f),
                MapMarker(3, 0.77f, 0.9f),
                MapMarker(4, 0.57f, 0.37f),
            ),
            route = listOf(1, 2, 3),
        ),
        ItineraryDay(
            number = 2,
            date = "13/10",
            title = "Bà Nà Hills",
            distance = "52 km",
            stops = listOf(
                ItineraryStop(
                    id = "golden-bridge",
                    time = "07:30",
                    name = "Cầu Vàng · Bà Nà Hills",
                    detail = "Tham quan · 4 giờ · vé chính thức",
                    cost = 900_000,
                    placeId = SamplePlaces.goldenBridge.id,
                ),
                ItineraryStop(
                    id = "bien-xanh-night-2",
                    time = "21:00",
                    name = "Khách sạn Biển Xanh",
                    detail = "Đêm 2/2 · Agoda",
                    cost = 300_000,
                    kind = StopKind.Lodging,
                ),
            ),
            markers = listOf(MapMarker(1, 0.18f, 0.82f)),
        ),
        ItineraryDay(
            number = 3,
            date = "14/10",
            title = "Phố cổ Hội An",
            distance = "30 km",
            stops = listOf(
                ItineraryStop(
                    id = "hoi-an",
                    time = "09:00",
                    name = "Phố cổ Hội An",
                    detail = "Tham quan · 4 giờ · vé chính thức",
                    cost = 120_000,
                ),
            ),
            markers = listOf(MapMarker(1, 0.62f, 0.88f)),
        ),
    )

    /** Place the user has just added in the editor ("Mới thêm" in Figma). */
    private val diving = ItineraryStop(
        id = "son-tra-diving",
        time = "14:00",
        name = "Lặn ngắm san hô Sơn Trà",
        detail = "3 giờ · Klook",
        cost = 700_000,
        isNew = true,
    )

    /**
     * What the editor opens with: [current] with the diving trip in place of "Cầu Rồng & Sơn Trà",
     * as in the Figma frame "Tự thiết kế lộ trình" (which goes over budget by 120.000đ).
     */
    fun editDraft(current: List<ItineraryDay>): List<ItineraryDay> {
        val first = current.first()
        if (first.stops.any { it.id == diving.id }) return current
        val stops = first.stops.toMutableList()
        val index = stops.indexOfFirst { it.id == dragonBridge.id }
        if (index >= 0) stops[index] = diving else stops.add(minOf(2, stops.size), diving)
        return listOf(first.copy(stops = stops, distance = "22 km")) + current.drop(1)
    }
}
