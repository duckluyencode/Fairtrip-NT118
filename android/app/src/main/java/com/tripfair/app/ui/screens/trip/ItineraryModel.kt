package com.tripfair.app.ui.screens.trip

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tripfair.app.R
import com.tripfair.app.data.places.LocationDto
import com.tripfair.app.data.trips.ItineraryDto
import com.tripfair.app.data.trips.LodgingDto
import com.tripfair.app.data.trips.StopPriceDto
import com.tripfair.app.ui.screens.explore.categoryLabel
import com.tripfair.app.ui.screens.explore.distanceText
import com.tripfair.app.ui.screens.explore.durationText
import java.time.LocalDate
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class StopKind { Place, Lodging }

/** One row of the day timeline. Costs are per person, in dong. */
data class ItineraryStop(
    val id: String,
    val time: String,
    val name: String,
    /** Line under the name, e.g. "Ăn uống · 45 phút · Ước tính · Google". */
    val detail: String,
    /** 0 means free. */
    val cost: Long,
    /** True when the price is not an official ticket price, so it is shown with "~". */
    val estimated: Boolean = false,
    val kind: StopKind = StopKind.Place,
    /** Place opened when the stop is tapped; null when there is no detail page. */
    val placeId: String? = null,
    /** Shown as "Mới thêm" in the editor. */
    val isNew: Boolean = false,
    /** Your spending after this stop, as the backend counted it; null in the editor's draft. */
    val runningTotal: Long? = null,
    /** In a group, you do not go to this stop and pay nothing for it. */
    val skipped: Boolean = false,
)

/** Numbered stop on the day's diagram; [x] and [y] are fractions of its size. */
data class MapMarker(val number: Int, val x: Float, val y: Float)

data class ItineraryDay(
    val number: Int,
    /** Short date on the selected day chip, e.g. "12/10". */
    val date: String,
    val title: String,
    val distance: String,
    val stops: List<ItineraryStop>,
    val markers: List<MapMarker>,
    /** Marker numbers joined by the route line. */
    val route: List<Int> = emptyList(),
)

/**
 * Places [locations] in a box: the stops keep their relative positions (north up), with a
 * margin so markers stay inside. A single stop sits in the middle.
 */
fun markersFor(locations: List<LocationDto>, margin: Float = 0.12f): List<MapMarker> {
    if (locations.isEmpty()) return emptyList()
    val minLat = locations.minOf { it.lat }
    val maxLat = locations.maxOf { it.lat }
    val minLng = locations.minOf { it.lng }
    val maxLng = locations.maxOf { it.lng }
    val span = 1f - 2 * margin
    return locations.mapIndexed { index, location ->
        val x = if (maxLng > minLng) ((location.lng - minLng) / (maxLng - minLng)).toFloat() else 0.5f
        val y = if (maxLat > minLat) ((maxLat - location.lat) / (maxLat - minLat)).toFloat() else 0.5f
        MapMarker(number = index + 1, x = margin + x * span, y = margin + y * span)
    }
}

/** Straight-line distance in km along [locations], in order (roads are longer). */
fun pathKm(locations: List<LocationDto>): Double = locations.zipWithNext().sumOf { (a, b) ->
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
    2 * 6371.0 * asin(sqrt(h))
}

/** "Vé · Klook", "Giá · Google" or "Ước tính · TripFair": always shown with a price. */
@Composable
fun priceSourceLabel(source: String, provider: String): String = when (source) {
    "PARTNER_API" -> stringResource(R.string.price_label_partner, provider)
    "GOOGLE_API" -> stringResource(R.string.price_label_google)
    else -> stringResource(R.string.price_label_estimated, provider)
}

@Composable
private fun StopPriceDto.label(): String = priceSourceLabel(source, provider)

/**
 * The backend's itinerary as timeline days. Your room comes first on day 1, since its cost
 * is taken from the budget before the places.
 */
@Composable
fun itineraryDays(itinerary: ItineraryDto): List<ItineraryDay> = itinerary.days.map { day ->
    val date = LocalDate.parse(day.date)
    val lodgingRow = itinerary.lodging?.takeIf { day.day == 1 }?.let { lodgingStop(it) }
    val places = day.stops.map { stop ->
        val parts = listOfNotNull(
            stop.category?.let { stringResource(categoryLabel(it)) },
            durationText(stop.visitMinutes),
            stop.price.label(),
            stop.attribution,
        )
        ItineraryStop(
            id = stop.stopId.toString(),
            time = stop.time,
            name = stop.name ?: stringResource(R.string.itinerary_place_fallback),
            detail = if (stop.myCost == null) stringResource(R.string.itinerary_skipped) else parts.joinToString(" · "),
            cost = stop.myCost ?: 0L,
            estimated = stop.price.isEstimate,
            placeId = stop.placeId.toString(),
            runningTotal = stop.runningTotal,
            skipped = stop.myCost == null,
        )
    }
    val locations = day.stops.mapNotNull { it.location }
    ItineraryDay(
        number = day.day,
        date = "${date.dayOfMonth}/${date.monthValue}",
        title = stringResource(weekdayName(date.dayOfWeek.value)),
        distance = stringResource(R.string.itinerary_distance, distanceText(pathKm(locations))),
        stops = listOfNotNull(lodgingRow) + places,
        markers = markersFor(locations),
        route = locations.indices.map { it + 1 },
    )
}

@Composable
private fun lodgingStop(lodging: LodgingDto): ItineraryStop = ItineraryStop(
    id = "lodging-${lodging.placeId}",
    time = "",
    name = lodging.name ?: stringResource(R.string.itinerary_lodging_fallback),
    detail = stringResource(R.string.itinerary_lodging_detail, lodging.nights, priceSourceLabel(lodging.priceSource, lodging.provider)),
    cost = lodging.myShare,
    estimated = lodging.isEstimate,
    kind = StopKind.Lodging,
    placeId = lodging.placeId.toString(),
    runningTotal = lodging.myShare,
)

private fun weekdayName(isoDay: Int): Int = when (isoDay) {
    1 -> R.string.weekday_mon
    2 -> R.string.weekday_tue
    3 -> R.string.weekday_wed
    4 -> R.string.weekday_thu
    5 -> R.string.weekday_fri
    6 -> R.string.weekday_sat
    else -> R.string.weekday_sun
}
