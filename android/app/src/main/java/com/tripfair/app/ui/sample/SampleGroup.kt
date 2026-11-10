package com.tripfair.app.ui.sample


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
}
