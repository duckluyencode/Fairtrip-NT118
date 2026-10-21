package com.tripfair.app.ui.sample

import androidx.annotation.DrawableRes
import com.tripfair.app.R

/**
 * A group member. [budget] is only known for the current user: other members' budgets
 * stay private (CLAUDE.md group rules), so the UI shows "Đã đặt" for them.
 */
data class SampleMember(
    val name: String,
    val preferences: String,
    val isYou: Boolean = false,
    val budget: String? = null,
)

data class TravelPreference(val label: String, @param:DrawableRes val icon: Int)

object SampleGroup {
    const val NAME = "Nhóm Đà Nẵng"
    const val CODE = "DN1214"
    const val INVITE_LINK = "https://tripfair.example/nhom/DN1214"
    const val DESTINATION_LINE = "Đà Nẵng · 12 – 14 tháng 10 · 3 ngày"
    const val SHORT_DETAIL = "12–14 tháng 10 · 3 ngày · 4 người"
    const val VOTE_DEADLINE = "20:00 hôm nay"
    const val YOUR_BUDGET = "2500000"

    val members = listOf(
        SampleMember("Đức", "Ẩm thực · Biển", isYou = true, budget = "2.500.000đ"),
        SampleMember("An", "Cảnh đẹp · Thư giãn"),
        SampleMember("Linh", "Vui chơi · Chụp ảnh"),
        SampleMember("Khoa", "Ẩm thực · Văn hóa"),
    )

    val preferences = listOf(
        TravelPreference("Ẩm thực", R.drawable.ic_utensils),
        TravelPreference("Biển", R.drawable.ic_waves),
        TravelPreference("Cảnh đẹp", R.drawable.ic_mountain),
        TravelPreference("Thư giãn", R.drawable.ic_leaf),
        TravelPreference("Vui chơi", R.drawable.ic_sparkles),
        TravelPreference("Chụp ảnh", R.drawable.ic_camera),
        TravelPreference("Văn hóa", R.drawable.ic_landmark),
        TravelPreference("Khám phá", R.drawable.ic_compass),
    )

    /** Preferences the current user picked. */
    val yourPreferences = setOf("Ẩm thực", "Biển")
}
