package com.tripfair.app.ui.sample

/** A suggestion while typing; [isPlace] picks the pin icon, otherwise the search icon. */
data class SearchSuggestion(val title: String, val subtitle: String, val isPlace: Boolean)

data class SearchHistoryItem(val id: Int, val query: String, val detail: String)

data class SearchHistoryGroup(val title: String, val items: List<SearchHistoryItem>)

data class SampleReview(
    val initials: String,
    val author: String,
    val stars: Int,
    val text: String,
    val tags: String,
    val hasPhoto: Boolean,
)

object SampleSearch {
    const val QUERY = "Đà Nẵng"

    val suggestions = listOf(
        SearchSuggestion("Đà Nẵng", "Thành phố · Việt Nam", isPlace = true),
        SearchSuggestion("Cầu Vàng", "Bà Nà Hills · Đà Nẵng", isPlace = true),
        SearchSuggestion("Ăn uống ở Đà Nẵng", "Từ khóa gợi ý", isPlace = false),
    )

    val recent = listOf("Bãi biển Mỹ Khê")

    val history = listOf(
        SearchHistoryGroup(
            "Hôm nay",
            listOf(
                SearchHistoryItem(1, "Cầu Vàng", "Bà Nà Hills · 09:20"),
                SearchHistoryItem(2, "Đà Nẵng", "Tất cả địa điểm · 09:12"),
                SearchHistoryItem(3, "Mì Quảng Bà Mua", "Ăn uống · 08:45"),
            ),
        ),
        SearchHistoryGroup(
            "Hôm qua",
            listOf(
                SearchHistoryItem(4, "Bãi biển Mỹ Khê", "Cảnh đẹp · 19:30"),
                SearchHistoryItem(5, "Chợ đêm Sơn Trà", "Vui chơi · 18:15"),
            ),
        ),
        SearchHistoryGroup(
            "7 ngày trước",
            listOf(SearchHistoryItem(6, "Bà Nà Hills", "Vui chơi · 1 tháng 10")),
        ),
    )

    // Figma marks these as illustrative content; real reviews come from TripFair users later.
    val reviews = listOf(
        SampleReview(
            initials = "DL",
            author = "Du khách · Minh họa",
            stars = 5,
            text = "Cảnh mây và núi rất đẹp. Cả nhóm có nhiều ảnh đẹp và thời gian ngắm cảnh trên cầu.",
            tags = "Ngắm cảnh · Đi cùng bạn bè",
            hasPhoto = true,
        ),
        SampleReview(
            initials = "ND",
            author = "Người đi du lịch · Minh họa",
            stars = 4,
            text = "Một trải nghiệm đáng thử ở Bà Nà Hills. Nên thống nhất lịch trình trước với cả nhóm.",
            tags = "Lịch trình · Đi theo nhóm",
            hasPhoto = false,
        ),
    )
}
