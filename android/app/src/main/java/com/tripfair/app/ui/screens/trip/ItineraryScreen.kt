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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.annotation.StringRes
import com.tripfair.app.R
import com.tripfair.app.data.trips.ItineraryWarningDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.ConfirmDialog
import com.tripfair.app.ui.components.EmptyState
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleItinerary
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun ItineraryRoute(
    tripId: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPlace: (placeId: String) -> Unit,
    onPlan: (group: Boolean) -> Unit,
    onDeleted: () -> Unit,
    viewModel: ItineraryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    // Again on each return, e.g. after the itinerary was edited.
    LaunchedEffect(tripId) { viewModel.start(tripId) }
    LaunchedEffect(viewModel.deleted) {
        if (viewModel.deleted) onDeleted()
    }
    val itinerary = viewModel.itinerary
    val trip = viewModel.trip
    if (itinerary == null || trip == null) {
        val error = viewModel.error
        TripLoadScaffold(
            title = stringResource(R.string.itinerary_title_short),
            loading = viewModel.loading,
            error = error?.let { tripErrorMessage(it) },
            onRetry = viewModel::load,
            onBack = onBack,
            onPlan = if (error?.code == "ITINERARY_NOT_READY") ({ onPlan(trip?.isGroup == true) }) else null,
        )
        return
    }
    ItineraryScreen(
        title = stringResource(R.string.itinerary_title, trip.name ?: trip.city, trip.days),
        days = itineraryDays(itinerary),
        budget = itinerary.me.budget,
        nights = itinerary.lodging?.nights ?: 0,
        warnings = itinerary.warnings,
        error = viewModel.saveError?.let { tripErrorMessage(it) },
        onBack = onBack,
        onEdit = onEdit,
        onPlace = onPlace,
        // Only the owner can delete a trip; members leave from the group screen.
        onDelete = if (trip.isOwner) viewModel::deleteTrip else null,
    )
}

/**
 * "Lộ trình theo ngày": day chips, the route map, your budget and the timeline of the day.
 * "⋮ → Sửa lộ trình" opens the editor; tapping a stop opens its place detail when there is one.
 */
@Composable
fun ItineraryScreen(
    title: String,
    days: List<ItineraryDay>,
    budget: Long,
    nights: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPlace: (placeId: String) -> Unit,
    warnings: List<ItineraryWarningDto> = emptyList(),
    @StringRes error: Int? = null,
    onDelete: (() -> Unit)? = null,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (confirmDelete && onDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.trip_delete_title),
            text = stringResource(R.string.trip_delete_text),
            confirmLabel = stringResource(R.string.trip_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
            confirmColor = MaterialTheme.colorScheme.error,
        )
    }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val dayIndex = selected.coerceIn(0, days.lastIndex)
    val day = days[dayIndex]

    Scaffold(
        topBar = {
            TripFairTopBar(title = title, onBack = onBack) {
                ItineraryMenu(onEdit = onEdit, onDelete = onDelete?.let { { confirmDelete = true } })
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
            if (day.markers.isNotEmpty()) RouteMap(day = day, onPlace = onPlace)
            DayHeader(day = day)
            TripBudgetCard(days = days, day = day, editing = false, budget = budget, nights = nights)
            WarningsCard(warnings = warnings, day = day.number)
            error?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (day.stops.isEmpty()) EmptyState(text = stringResource(R.string.itinerary_day_empty))
                var total = days.costBefore(dayIndex)
                day.stops.forEach { stop ->
                    total += stop.cost
                    TimelineRow(
                        stop = stop,
                        // The backend's total, which counts what you pay where; else added up here.
                        runningTotal = stop.runningTotal ?: total,
                        onClick = stop.placeId?.let { id -> { onPlace(id) } },
                    )
                }
            }
        }
    }
}

/** What the planner flagged for the whole trip or for [day]: closed places, long days, budgets. */
@Composable
private fun WarningsCard(warnings: List<ItineraryWarningDto>, day: Int) {
    val shown = warnings.filter { it.day == null || it.day == day }.distinctBy { it.code }
    if (shown.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(colors.errorContainer, MaterialTheme.shapes.small)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        shown.forEach { warning ->
            val text = when (warning.code) {
                "DAY_TOO_LONG" -> stringResource(R.string.itinerary_warning_day_too_long)
                "PLACE_CLOSED" -> stringResource(R.string.itinerary_warning_place_closed)
                "OVER_BUDGET" -> stringResource(R.string.itinerary_warning_over_budget)
                "MEMBER_OVER_BUDGET" -> stringResource(R.string.itinerary_warning_member_over)
                else -> null
            }
            text?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.onErrorContainer) }
        }
    }
}

/** "⋮" button with "Sửa lộ trình" and, for the trip's owner, "Xóa chuyến đi". */
@Composable
private fun ItineraryMenu(onEdit: () -> Unit, onDelete: (() -> Unit)?) {
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
            if (onDelete != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.trip_delete),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        expanded = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ItineraryScreenPreview() {
    TripFairTheme {
        ItineraryScreen(
            title = SampleItinerary.TITLE,
            days = SampleItinerary.days,
            budget = SampleItinerary.BUDGET,
            nights = SampleItinerary.NIGHTS,
            onBack = {},
            onEdit = {},
            onPlace = {},
        )
    }
}
