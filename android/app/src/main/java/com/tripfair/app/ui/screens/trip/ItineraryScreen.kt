package com.tripfair.app.ui.screens.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tripfair.app.R
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.ItineraryDay
import com.tripfair.app.ui.sample.SampleItinerary
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/**
 * "Lộ trình theo ngày": day chips, the route map, your budget and the timeline of the day.
 * "⋮ → Sửa lộ trình" opens the editor; tapping a stop opens its place detail when there is one.
 */
@Composable
fun ItineraryScreen(
    days: List<ItineraryDay>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPlace: (placeId: String) -> Unit,
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val dayIndex = selected.coerceIn(0, days.lastIndex)
    val day = days[dayIndex]

    Scaffold(
        topBar = {
            TripFairTopBar(title = SampleItinerary.TITLE, onBack = onBack) {
                ItineraryMenu(onEdit = onEdit)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DayChips(days = days, selected = dayIndex, onSelect = { selected = it })
            RouteMap(day = day)
            DayHeader(day = day)
            TripBudgetCard(days = days, day = day, editing = false)
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                var total = days.costBefore(dayIndex)
                day.stops.forEach { stop ->
                    total += stop.cost
                    TimelineRow(
                        stop = stop,
                        runningTotal = total,
                        onClick = stop.placeId?.let { id -> { onPlace(id) } },
                    )
                }
            }
        }
    }
}

/** "⋮" button with the "Sửa lộ trình" item. */
@Composable
private fun ItineraryMenu(onEdit: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_ellipsis),
                contentDescription = stringResource(R.string.cd_itinerary_menu),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(90f),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.itinerary_edit), style = MaterialTheme.typography.bodyLarge) },
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ItineraryScreenPreview() {
    TripFairTheme {
        ItineraryScreen(days = SampleItinerary.days, onBack = {}, onEdit = {}, onPlace = {})
    }
}
