package com.tripfair.app.ui.sample

import androidx.annotation.DrawableRes
import com.tripfair.app.R

/** A place suggested from the photo. [placeId] is set when the sample data has its detail page. */
data class PhotoSuggestion(
    val name: String,
    val detail: String,
    @param:DrawableRes val image: Int,
    val inTrip: Boolean,
    val placeId: String? = null,
)

/** What Google Cloud Vision found in a photo ("Kết quả nhận diện" in Figma). */
data class PhotoRecognition(
    val recognized: Boolean,
    @param:DrawableRes val photo: Int,
    val title: String,
    val subtitle: String,
    /** "96%", only when a landmark was recognized. */
    val confidence: String?,
    val labels: List<String>,
    val suggestions: List<PhotoSuggestion>,
    /** Landmark opened by tapping the result card. */
    val placeId: String? = null,
)

object SampleCamera {
    const val TRIP_NAME = "Đà Nẵng"

    /** Result of the shutter button: the Golden Bridge is recognized. */
    val recognized = PhotoRecognition(
        recognized = true,
        photo = R.drawable.sample_golden_bridge,
        title = "Cầu Vàng",
        subtitle = "Bà Nà Hills · Hòa Vang",
        confidence = "96%",
        labels = listOf("cây cầu", "núi", "kiến trúc", "du lịch"),
        suggestions = listOf(
            PhotoSuggestion("Bà Nà Hills", "Cùng khu du lịch · 0,4 km", R.drawable.sample_golden_bridge_card, inTrip = true),
            PhotoSuggestion("Vườn hoa Le Jardin", "Cảnh đẹp tương tự · 0,8 km", R.drawable.sample_golden_bridge_square, inTrip = false),
            PhotoSuggestion(
                "Bãi biển Mỹ Khê",
                "Có trong lịch trình ngày 1 · 27 km",
                R.drawable.sample_my_khe,
                inTrip = false,
                placeId = SamplePlaces.myKhe.id,
            ),
        ),
        placeId = SamplePlaces.goldenBridge.id,
    )

    /** Result of a photo picked from the library: no landmark, suggestions come from the labels. */
    val notRecognized = PhotoRecognition(
        recognized = false,
        photo = R.drawable.sample_my_khe_sunset,
        title = "Chưa xác định được địa danh",
        subtitle = "Ảnh có thể quá tối hoặc bị che khuất",
        confidence = null,
        labels = listOf("biển", "bầu trời", "hoàng hôn", "du lịch"),
        suggestions = listOf(
            PhotoSuggestion(
                "Bãi biển Mỹ Khê",
                "Biển, hoàng hôn · 2,1 km",
                R.drawable.sample_my_khe,
                inTrip = true,
                placeId = SamplePlaces.myKhe.id,
            ),
            PhotoSuggestion("Bãi biển Non Nước", "Biển · 8 km", R.drawable.sample_my_khe_sunset, inTrip = false),
            PhotoSuggestion("Bán đảo Sơn Trà", "Ngắm hoàng hôn · 9 km", R.drawable.sample_trip_da_nang, inTrip = false),
        ),
    )
}
