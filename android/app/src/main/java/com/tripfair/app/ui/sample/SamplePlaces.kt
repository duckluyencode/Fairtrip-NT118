package com.tripfair.app.ui.sample

import androidx.annotation.DrawableRes
import com.tripfair.app.R

enum class PlaceCategory { Food, Fun, Scenic }

/** A place as the explore screens show it. Text is display-ready sample data. */
data class SamplePlace(
    val id: String,
    val name: String,
    /** Area under the name, e.g. "Bà Nà Hills · Hòa Vang". */
    val area: String,
    val categories: Set<PlaceCategory>,
    val categoryLabel: String,
    val rating: String,
    val reviewCount: String,
    val openingHours: String,
    val description: String,
    /** Price label and value; non-official prices start with "~" (CLAUDE.md price rules). */
    val priceLabel: String,
    val price: String,
    val duration: String,
    val distanceFromCenter: String,
    /** Second line in search results, e.g. "Bà Nà Hills · 25 km". */
    val resultLine: String,
    val groupHint: String,
    @param:DrawableRes val cardImage: Int,
    @param:DrawableRes val thumbImage: Int,
    @param:DrawableRes val heroImage: Int,
)

object SamplePlaces {
    val goldenBridge = SamplePlace(
        id = "golden-bridge",
        name = "Cầu Vàng",
        area = "Bà Nà Hills · Hòa Vang",
        categories = setOf(PlaceCategory.Scenic, PlaceCategory.Fun),
        categoryLabel = "Cảnh đẹp",
        rating = "4,8",
        reviewCount = "12.450",
        openingHours = "Mở cửa đến 22:00",
        description = "Cây cầu giữa mây nổi tiếng với đôi bàn tay khổng lồ. Phù hợp ngắm cảnh, chụp ảnh và trải nghiệm cáp treo cùng nhóm.",
        priceLabel = "Vé · chính thức",
        price = "900.000đ",
        duration = "4 giờ",
        distanceFromCenter = "25 km",
        resultLine = "Bà Nà Hills · 25 km",
        groupHint = "Hợp sở thích 3/4 thành viên · Có thể vượt ngân sách của 1 thành viên",
        cardImage = R.drawable.sample_golden_bridge_card,
        thumbImage = R.drawable.sample_golden_bridge_square,
        heroImage = R.drawable.sample_golden_bridge,
    )

    val miQuang = SamplePlace(
        id = "mi-quang-ba-mua",
        name = "Mì Quảng Bà Mua",
        area = "Trần Bình Trọng · Hải Châu",
        categories = setOf(PlaceCategory.Food),
        categoryLabel = "Ăn uống",
        rating = "4,6",
        reviewCount = "3.210",
        openingHours = "Mở cửa đến 21:00",
        description = "Quán mì Quảng lâu năm, tô đầy đặn với tôm, thịt và bánh tráng giòn. Hợp cho bữa trưa nhanh của cả nhóm.",
        priceLabel = "Ăn uống · Google",
        price = "~50.000đ",
        duration = "1 giờ",
        distanceFromCenter = "1,5 km",
        resultLine = "Ăn uống · 1,5 km",
        groupHint = "Hợp sở thích 4/4 thành viên · Trong ngân sách của cả nhóm",
        cardImage = R.drawable.sample_mi_quang,
        thumbImage = R.drawable.sample_mi_quang,
        heroImage = R.drawable.sample_mi_quang,
    )

    val myKhe = SamplePlace(
        id = "my-khe-beach",
        name = "Bãi biển Mỹ Khê",
        area = "Phước Mỹ · Sơn Trà",
        categories = setOf(PlaceCategory.Scenic),
        categoryLabel = "Cảnh đẹp",
        rating = "4,7",
        reviewCount = "8.930",
        openingHours = "Mở cả ngày",
        description = "Bãi biển cát trắng dài, nước trong và sóng êm. Hợp tắm biển buổi sáng và ngắm hoàng hôn cùng nhóm.",
        priceLabel = "Vé",
        price = "Miễn phí",
        duration = "2 giờ",
        distanceFromCenter = "2,1 km",
        resultLine = "Sơn Trà · 2,1 km",
        groupHint = "Hợp sở thích 4/4 thành viên · Miễn phí cho cả nhóm",
        cardImage = R.drawable.sample_my_khe,
        thumbImage = R.drawable.sample_my_khe,
        heroImage = R.drawable.sample_my_khe,
    )

    val nightMarket = SamplePlace(
        id = "son-tra-night-market",
        name = "Chợ đêm Sơn Trà",
        area = "Mai Hắc Đế · Sơn Trà",
        categories = setOf(PlaceCategory.Food, PlaceCategory.Fun),
        categoryLabel = "Vui chơi",
        rating = "4,5",
        reviewCount = "5.120",
        openingHours = "Mở cửa đến 23:30",
        description = "Khu chợ đêm cạnh cầu Rồng với nhiều quầy hải sản và đồ lưu niệm. Hợp dạo chơi buổi tối sau khi xem cầu Rồng phun lửa.",
        priceLabel = "Ăn uống · ước tính",
        price = "~250.000đ",
        duration = "2 giờ",
        distanceFromCenter = "1,8 km",
        resultLine = "Hải sản · 1,8 km",
        groupHint = "Hợp sở thích 3/4 thành viên · Trong ngân sách của cả nhóm",
        cardImage = R.drawable.sample_night_market,
        thumbImage = R.drawable.sample_night_market,
        heroImage = R.drawable.sample_night_market,
    )

    val all = listOf(goldenBridge, miQuang, myKhe, nightMarket)

    /** Places suggested for the group on the home screen. */
    val groupSuggestions = listOf(goldenBridge, miQuang)

    /** Results for the sample query "Đà Nẵng". */
    val searchResults = listOf(goldenBridge, myKhe, nightMarket)

    /** Places saved in "Yêu thích". */
    val favorites = listOf(goldenBridge, myKhe, nightMarket, miQuang)

    fun byId(id: String): SamplePlace = all.firstOrNull { it.id == id } ?: goldenBridge
}

/** The group trip shown on the home screen. */
object SampleTrip {
    const val NAME = "Đà Nẵng, mình đi thôi!"
    const val DATES = "12–14 tháng 10 · 3 ngày 2 đêm"
    const val DATE_RANGE = "12–14 tháng 10"
    const val MEMBER_COUNT = 4
}
