package com.tripfair.app.ui.screens.group

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tripfair.app.R
import com.tripfair.app.data.trips.Interest

/** A travel preference chip ("Sở thích du lịch") and the backend interest it stands for. */
data class InterestOption(val code: String, @param:StringRes val label: Int, @param:DrawableRes val icon: Int)

val interestOptions = listOf(
    InterestOption(Interest.FOOD, R.string.interest_food, R.drawable.ic_utensils),
    InterestOption(Interest.BEACH, R.string.interest_beach, R.drawable.ic_waves),
    InterestOption(Interest.SCENERY, R.string.interest_scenery, R.drawable.ic_mountain),
    InterestOption(Interest.RELAX, R.string.interest_relax, R.drawable.ic_leaf),
    InterestOption(Interest.FUN, R.string.interest_fun, R.drawable.ic_sparkles),
    InterestOption(Interest.PHOTO, R.string.interest_photo, R.drawable.ic_camera),
    InterestOption(Interest.CULTURE, R.string.interest_culture, R.drawable.ic_landmark),
    InterestOption(Interest.EXPLORE, R.string.interest_explore, R.drawable.ic_compass),
)

/** "Ẩm thực · Biển", or "Chưa chọn sở thích". */
@Composable
fun interestsText(codes: List<String>): String {
    val labels = interestOptions.filter { it.code in codes }.map { stringResource(it.label) }
    return if (labels.isEmpty()) stringResource(R.string.interests_none) else labels.joinToString(" · ")
}
