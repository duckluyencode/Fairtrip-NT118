package com.tripfair.app.ui.screens.explore

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tripfair.app.R
import com.tripfair.app.data.network.ApiError
import com.tripfair.app.data.places.OpeningHoursDto
import com.tripfair.app.data.places.PriceDto
import com.tripfair.app.ui.components.formatVnd
import java.util.Locale

// Turns backend place data into the Vietnamese text the screens show.

private val Vietnamese = Locale.forLanguageTag("vi-VN")

/** 4.8 -> "4,8" */
fun ratingText(rating: Double): String = String.format(Vietnamese, "%.1f", rating)

/** 2.5 -> "2,5 km" */
fun distanceText(km: Double): String = String.format(Vietnamese, "%.1f km", km)

/** Beyond this, "how far from me" says nothing useful about a place (the user is not there yet). */
private const val NEARBY_KM = 300.0

/** The distance from the user for a list row, or null when unknown or too far to matter. */
fun nearbyDistanceText(km: Double?): String? = km?.takeIf { it <= NEARBY_KM }?.let(::distanceText)

/** 12450 -> "12.450" */
fun countText(count: Int): String = String.format(Vietnamese, "%,d", count)

@StringRes
fun categoryLabel(category: String): Int = when (category) {
    "FOOD" -> R.string.filter_food
    "SCENIC" -> R.string.filter_scenic
    else -> R.string.filter_fun
}

/** "~50.000đ–150.000đ" for estimates, "900.000đ" for official tickets, "Miễn phí" for free. */
@Composable
fun priceValue(price: PriceDto): String {
    if (price.max == 0) return stringResource(R.string.price_free)
    val amount = if (price.min == price.max) {
        formatVnd(price.max.toLong())
    } else {
        "${formatVnd(price.min.toLong())}–${formatVnd(price.max.toLong())}"
    }
    return if (price.isEstimate) "~$amount" else amount
}

/** Where the price comes from; always shown next to it. */
@Composable
fun priceLabel(price: PriceDto): String = when {
    price.includedIn != null -> stringResource(R.string.price_label_included, price.includedIn.name)
    price.source == "PARTNER_API" -> stringResource(R.string.price_label_partner, price.provider)
    price.source == "GOOGLE_API" -> stringResource(R.string.price_label_google)
    else -> stringResource(R.string.price_label_estimated, price.provider)
}

@Composable
fun hoursText(hours: OpeningHoursDto?): String = when {
    hours == null -> stringResource(R.string.hours_unknown)
    hours.closedToday -> stringResource(R.string.hours_closed_today)
    hours.allDay -> stringResource(R.string.hours_all_day)
    else -> stringResource(R.string.hours_range, hours.opensAt.orEmpty(), hours.closesAt.orEmpty())
}

@Composable
fun durationText(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> stringResource(R.string.duration_minutes, rest)
        rest == 0 -> stringResource(R.string.duration_hours, hours)
        else -> stringResource(R.string.duration_hours_minutes, hours, rest)
    }
}

@StringRes
fun loadErrorMessage(error: ApiError): Int = when (error.code) {
    ApiError.NETWORK -> R.string.error_network
    "PLACE_NOT_FOUND" -> R.string.error_place_not_found
    "GOOGLE_PLACES_UNAVAILABLE" -> R.string.error_google_unavailable
    else -> R.string.error_unexpected
}
