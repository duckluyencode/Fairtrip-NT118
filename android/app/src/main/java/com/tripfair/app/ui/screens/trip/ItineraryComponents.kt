package com.tripfair.app.ui.screens.trip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalInspectionMode
import com.tripfair.app.BuildConfig
import com.tripfair.app.R
import com.tripfair.app.ui.components.PlacesMap
import com.tripfair.app.ui.components.ProgressBar
import com.tripfair.app.ui.components.TripFairChip
import com.tripfair.app.ui.components.formatVnd

/** Total cost per person of the whole trip. */
internal fun List<ItineraryDay>.totalCost(): Long = sumOf { day -> day.stops.sumOf { it.cost } }

/** Cost of the stops before day [index], so running totals continue across days. */
internal fun List<ItineraryDay>.costBefore(index: Int): Long = take(index).totalCost()

/** "~50.000đ", "300.000đ" or "Miễn phí"; "~" marks prices that are not official. */
@Composable
internal fun ItineraryStop.costLabel(): String = when {
    skipped -> stringResource(R.string.itinerary_not_going)
    pricePending -> stringResource(R.string.itinerary_price_pending)
    cost == 0L -> stringResource(R.string.itinerary_free)
    estimated -> "~" + formatVnd(cost)
    else -> formatVnd(cost)
}

/** "Ngày 1 · 12/10", "Ngày 2", "Ngày 3": the selected day shows its date. */
@Composable
internal fun DayChips(days: List<ItineraryDay>, selected: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEachIndexed { index, day ->
            TripFairChip(
                text = if (index == selected) {
                    stringResource(R.string.itinerary_day_chip_selected, day.number, day.date)
                } else {
                    stringResource(R.string.itinerary_day_chip, day.number)
                },
                selected = index == selected,
                onClick = { onSelect(index) },
            )
        }
    }
}

/** "Ngày 1 · Biển & ẩm thực" with the day's cost and distance on the right. */
@Composable
internal fun DayHeader(day: ItineraryDay) {
    Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.itinerary_day_title, day.number, day.title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.itinerary_day_summary, formatVnd(day.stops.sumOf { it.cost }), day.distance),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * "Cả chuyến của bạn": spending against your [budget] (lodging for [nights] included). Over
 * budget the card turns red; in the editor it then suggests which stop of [day] to drop (the
 * cheapest one that is enough).
 */
@Composable
internal fun TripBudgetCard(days: List<ItineraryDay>, day: ItineraryDay, editing: Boolean, budget: Long, nights: Int) {
    val colors = MaterialTheme.colorScheme
    val used = days.totalCost()
    val over = used - budget
    val isOver = over > 0
    val shape = MaterialTheme.shapes.small
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(if (isOver) colors.tertiaryContainer else colors.surfaceContainerLow, shape)
            .then(if (isOver) Modifier.border(1.dp, colors.error, shape) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.itinerary_trip_total),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.itinerary_used_of, formatVnd(used), formatVnd(budget)),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        ProgressBar(
            // A 0đ budget only allows free places: the bar is empty, or full once anything costs.
            fraction = if (budget > 0) used.toFloat() / budget else if (used > 0) 1f else 0f,
            height = 7.dp,
            color = if (isOver) colors.error else colors.primary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isOver) {
                    stringResource(R.string.itinerary_over, formatVnd(over))
                } else {
                    stringResource(R.string.itinerary_left, formatVnd(-over))
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isOver) colors.error else colors.primary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.itinerary_no_transport),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
        if (isOver && editing) {
            val drop = day.stops
                .filter { it.kind == StopKind.Place && it.cost >= over }
                .minByOrNull { it.cost }
            Text(
                text = if (drop != null) {
                    stringResource(R.string.itinerary_suggest_drop, drop.name, drop.costLabel())
                } else {
                    stringResource(R.string.itinerary_suggest_generic)
                },
                style = MaterialTheme.typography.labelSmall,
            )
        } else {
            val lodging = days.sumOf { d -> d.stops.filter { it.kind == StopKind.Lodging }.sumOf { it.cost } }
            Text(
                text = if (lodging > 0) {
                    stringResource(R.string.itinerary_breakdown, formatVnd(lodging), nights, formatVnd(used - lodging))
                } else {
                    stringResource(R.string.itinerary_breakdown_self, formatVnd(used))
                },
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/**
 * The day's stops joined in visiting order ("Bản đồ lộ trình"): on Google Maps when the stops
 * have coordinates and the app has a Maps key, else (previews, builds without a key) as
 * numbered markers on a plain background.
 */
@Composable
internal fun RouteMap(day: ItineraryDay, onPlace: ((placeId: String) -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.cd_itinerary_map, day.number)
    if (day.pins.isNotEmpty() && BuildConfig.HAS_MAPS_KEY && !LocalInspectionMode.current) {
        // The real map: numbered stops joined in visiting order.
        PlacesMap(
            pins = day.pins,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .semantics { contentDescription = description },
            route = true,
            onPinClick = onPlace?.let { open -> { pin -> open(pin.id) } },
        )
        return
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = description },
    ) {
        // Not a map: the stops keep their relative positions (north up) on a plain background.
        Box(modifier = Modifier.fillMaxSize().background(colors.surfaceContainerHigh))
        val routeColor = colors.primary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val points = day.route.mapNotNull { number -> day.markers.firstOrNull { it.number == number } }
            points.zipWithNext().forEach { (from, to) ->
                drawLine(
                    color = routeColor,
                    start = Offset(from.x * size.width, from.y * size.height),
                    end = Offset(to.x * size.width, to.y * size.height),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        day.markers.forEach { marker ->
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * marker.x - 16.dp, y = maxHeight * marker.y - 16.dp)
                    .size(32.dp)
                    .background(colors.tertiary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = marker.number.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = colors.onTertiary,
                )
            }
        }
    }
}

/**
 * One timeline row: time, marker (coral for places, teal for lodging and new places),
 * name and details, then the cost and the running total ("Điểm lịch trình").
 */
@Composable
internal fun TimelineRow(
    stop: ItineraryStop,
    runningTotal: Long,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stop.time,
            style = MaterialTheme.typography.bodySmall,
            color = colors.primary,
            modifier = Modifier.width(42.dp),
        )
        Box(modifier = Modifier.width(10.dp).height(52.dp), contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .padding(top = 13.dp)
                    .width(2.dp)
                    .height(39.dp)
                    .background(colors.outline),
            )
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(10.dp)
                    .background(if (stop.kind == StopKind.Lodging || stop.isNew) colors.primary else colors.tertiary, CircleShape),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stop.name, style = MaterialTheme.typography.bodyLarge)
            val newLabel = stringResource(R.string.itinerary_new)
            Text(
                text = buildAnnotatedString {
                    if (stop.isNew) {
                        withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold)) { append(newLabel) }
                        append(" · ")
                    }
                    append(stop.detail)
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stop.costLabel(), style = MaterialTheme.typography.labelMedium)
            Text(
                text = stringResource(R.string.itinerary_running_total, formatVnd(runningTotal)),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
