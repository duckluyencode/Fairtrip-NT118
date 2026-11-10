package com.tripfair.app.ui.screens.group

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.trips.TripSummaryDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.ErrorState
import com.tripfair.app.ui.components.LoadingState
import com.tripfair.app.ui.components.Chevron
import com.tripfair.app.ui.components.ListCard
import com.tripfair.app.ui.components.ListDivider
import com.tripfair.app.ui.components.ListRow
import com.tripfair.app.ui.components.MainTab
import com.tripfair.app.ui.components.MainTabBar
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.SectionHeader
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleTrip
import com.tripfair.app.ui.screens.trip.title
import com.tripfair.app.ui.screens.trip.tripDatesLine
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun TripsRoute(
    onCreateTrip: () -> Unit,
    onJoinGroup: () -> Unit,
    onOpenTrip: (TripSummaryDto) -> Unit,
    onSchedule: (TripSummaryDto) -> Unit,
    onInvite: (tripId: Int) -> Unit,
    onVote: (tripId: Int) -> Unit,
    onPlaces: () -> Unit,
    onTabSelect: (MainTab) -> Unit,
    viewModel: TripsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    // Each time the tab opens or the user comes back to it: trips may have been created or joined.
    LaunchedEffect(Unit) { viewModel.load() }
    GroupsScreen(
        trips = viewModel.items,
        loading = viewModel.loading,
        error = viewModel.error,
        onRetry = viewModel::load,
        onCreateTrip = onCreateTrip,
        onJoinGroup = onJoinGroup,
        onOpenTrip = onOpenTrip,
        onSchedule = onSchedule,
        onInvite = onInvite,
        onVote = onVote,
        onPlaces = onPlaces,
        onTabSelect = onTabSelect,
    )
}

/**
 * Root of the "Chuyến đi" tab: the user's solo and group trips, or the empty state
 * ("Chuyến đi · chưa có" in Figma) when there are none.
 */
@Composable
fun GroupsScreen(
    trips: List<TripSummaryDto>,
    loading: Boolean,
    @StringRes error: Int?,
    onRetry: () -> Unit,
    onCreateTrip: () -> Unit,
    onJoinGroup: () -> Unit,
    onOpenTrip: (TripSummaryDto) -> Unit,
    onSchedule: (TripSummaryDto) -> Unit,
    onInvite: (tripId: Int) -> Unit,
    onVote: (tripId: Int) -> Unit,
    onPlaces: () -> Unit,
    onTabSelect: (MainTab) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val sectionStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 26.sp)
    val firstGroup = trips.firstOrNull { it.isGroup }

    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.trips_empty_screen_title)) },
        bottomBar = { MainTabBar(selected = MainTab.Trips, onSelect = onTabSelect) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(if (trips.isEmpty()) R.string.trips_empty_intro else R.string.groups_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
            when {
                trips.isEmpty() && loading -> LoadingState()
                trips.isEmpty() && error != null -> ErrorState(message = stringResource(error), onRetry = onRetry)
                trips.isEmpty() -> EmptyTrips(onCreateTrip = onCreateTrip, onJoinGroup = onJoinGroup)
                else -> {
                    PrimaryButton(text = stringResource(R.string.trips_create), onClick = onCreateTrip, height = 52.dp)
                    Text(
                        text = stringResource(R.string.trips_join_link),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable(role = Role.Button, onClick = onJoinGroup)
                            .padding(4.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(R.string.trips_list), style = sectionStyle, modifier = Modifier.weight(1f))
                            Text(
                                text = pluralStringResource(R.plurals.trips_count, trips.size, trips.size),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.primary,
                            )
                        }
                        // A failed reload keeps the list and says so above it.
                        error?.let {
                            Text(text = stringResource(it), style = MaterialTheme.typography.bodySmall, color = colors.error)
                        }
                        trips.forEach { trip ->
                            TripCard(
                                trip = trip,
                                onOpen = { onOpenTrip(trip) },
                                onSchedule = { onSchedule(trip) },
                                onInvite = { onInvite(trip.id) },
                                onVote = { onVote(trip.id) },
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = stringResource(R.string.groups_prepare), style = sectionStyle)
                        ListCard(contentPadding = PaddingValues(14.dp)) {
                            ListRow(
                                icon = R.drawable.ic_map_pin,
                                title = stringResource(R.string.groups_places),
                                subtitle = stringResource(R.string.groups_places_detail),
                                onClick = onPlaces,
                                contentPadding = PaddingValues(vertical = 6.dp),
                            )
                            if (firstGroup != null) {
                                ListDivider()
                                ListRow(
                                    icon = R.drawable.ic_wallet,
                                    title = stringResource(R.string.groups_budgets),
                                    subtitle = stringResource(R.string.groups_budgets_of, firstGroup.title()),
                                    onClick = { onOpenTrip(firstGroup) },
                                    contentPadding = PaddingValues(vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One trip: what it is, when, and what is left to do; group trips add invite and vote. */
@Composable
private fun TripCard(trip: TripSummaryDto, onOpen: () -> Unit, onSchedule: () -> Unit, onInvite: () -> Unit, onVote: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.clickable(role = Role.Button, onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconTile(icon = if (trip.isGroup) R.drawable.ic_users else R.drawable.ic_map_pin, size = 48.dp, iconSize = 22.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = trip.title(), style = MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp, lineHeight = 24.sp))
                Text(
                    text = if (trip.isGroup) trip.city else stringResource(R.string.trip_type_solo),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primary,
                )
                Text(text = tripDatesLine(trip), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            Chevron()
        }
        if (trip.isGroup) {
            ListDivider()
            SectionHeader(
                title = pluralStringResource(R.plurals.groups_member_count, trip.memberCount, trip.memberCount),
                titleStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                actionLabel = stringResource(R.string.groups_invite),
                onAction = onInvite,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primaryContainer, MaterialTheme.shapes.extraSmall)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(if (trip.isPlanned) R.drawable.ic_circle_check else R.drawable.ic_calendar_days),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(17.dp),
            )
            val status = when {
                trip.isPlanned -> R.string.trip_status_planned
                trip.isGroup -> R.string.trip_status_group_draft
                else -> R.string.trip_status_solo_draft
            }
            Text(text = stringResource(status), style = MaterialTheme.typography.labelSmall, color = colors.primary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Shortcut(
                R.drawable.ic_calendar_days,
                stringResource(if (trip.isPlanned) R.string.groups_schedule else R.string.trip_continue_plan),
                onSchedule,
                Modifier.weight(1f),
            )
            if (trip.isGroup) Shortcut(R.drawable.ic_vote, stringResource(R.string.groups_vote), onVote, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Shortcut(@DrawableRes icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmptyTrips(onCreateTrip: () -> Unit, onJoinGroup: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(colors.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(R.drawable.ic_calendar_large), contentDescription = null, tint = colors.primary, modifier = Modifier.size(40.dp))
        }
        Text(
            text = stringResource(R.string.trips_empty_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, letterSpacing = 0.sp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.trips_empty_text),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            text = stringResource(R.string.trips_create),
            onClick = onCreateTrip,
            trailingIcon = R.drawable.ic_arrow_right,
            height = 52.dp,
        )
        Text(
            text = stringResource(R.string.trips_join_link),
            style = MaterialTheme.typography.titleSmall,
            color = colors.primary,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onJoinGroup)
                .padding(4.dp),
        )
    }
}

@ThemePreviews
@Composable
private fun GroupsScreenPreview() {
    TripFairTheme {
        GroupsScreen(
            trips = SampleTrip.summaries, loading = false, error = null, onRetry = {},
            onCreateTrip = {}, onJoinGroup = {}, onOpenTrip = {}, onSchedule = {}, onInvite = {}, onVote = {},
            onPlaces = {}, onTabSelect = {},
        )
    }
}

@ThemePreviews
@Composable
private fun TripsEmptyPreview() {
    TripFairTheme {
        GroupsScreen(
            trips = emptyList(), loading = false, error = null, onRetry = {},
            onCreateTrip = {}, onJoinGroup = {}, onOpenTrip = {}, onSchedule = {}, onInvite = {}, onVote = {},
            onPlaces = {}, onTabSelect = {},
        )
    }
}
