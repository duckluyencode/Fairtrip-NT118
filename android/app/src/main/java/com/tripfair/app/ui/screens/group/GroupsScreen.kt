package com.tripfair.app.ui.screens.group

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.Chevron
import com.tripfair.app.ui.components.ListCard
import com.tripfair.app.ui.components.ListDivider
import com.tripfair.app.ui.components.ListRow
import com.tripfair.app.ui.components.MainTab
import com.tripfair.app.ui.components.MainTabBar
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.SectionHeader
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleGroup
import com.tripfair.app.ui.sample.SampleTrip
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/**
 * Root of the "Chuyến đi" tab. Shows the user's groups, or the empty state
 * ("Chuyến đi · chưa có" in Figma) when [hasGroups] is false.
 */
@Composable
fun GroupsScreen(
    onNewGroup: () -> Unit,
    onOpenGroup: () -> Unit,
    onInvite: () -> Unit,
    onSchedule: () -> Unit,
    onVote: () -> Unit,
    onPlaces: () -> Unit,
    onCreateTrip: () -> Unit,
    onJoinGroup: () -> Unit,
    onTabSelect: (MainTab) -> Unit,
    hasGroups: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val sectionStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 26.sp)

    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(if (hasGroups) R.string.groups_title else R.string.trips_empty_screen_title))
        },
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
                text = stringResource(if (hasGroups) R.string.groups_intro else R.string.trips_empty_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
            if (!hasGroups) {
                EmptyTrips(onCreateTrip = onCreateTrip, onJoinGroup = onJoinGroup)
                return@Column
            }
            PrimaryButton(text = stringResource(R.string.groups_new), onClick = onNewGroup, height = 52.dp)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.groups_joined), style = sectionStyle, modifier = Modifier.weight(1f))
                    Text(
                        text = pluralStringResource(R.plurals.groups_count, 1, 1),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary,
                    )
                }
                GroupCard(onOpen = onOpenGroup, onInvite = onInvite, onSchedule = onSchedule, onVote = onVote)
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
                    ListDivider()
                    ListRow(
                        icon = R.drawable.ic_wallet,
                        title = stringResource(R.string.groups_budgets),
                        subtitle = stringResource(R.string.groups_budgets_detail),
                        onClick = onOpenGroup,
                        contentPadding = PaddingValues(vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCard(onOpen: () -> Unit, onInvite: () -> Unit, onSchedule: () -> Unit, onVote: () -> Unit) {
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
            Image(
                painter = painterResource(R.drawable.sample_trip_da_nang),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 58.dp, height = 68.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = SampleGroup.NAME, style = MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp, lineHeight = 24.sp))
                Text(
                    text = SampleTrip.NAME,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primary,
                )
                Text(text = SampleTrip.DATES, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            Chevron()
        }
        ListDivider()
        SectionHeader(
            title = pluralStringResource(R.plurals.groups_member_count, SampleGroup.members.size, SampleGroup.members.size),
            titleStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            actionLabel = stringResource(R.string.groups_invite),
            onAction = onInvite,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SampleGroup.members.forEachIndexed { index, member ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MemberAvatar(index = index, size = 22.dp)
                    Text(text = member.name, style = MaterialTheme.typography.labelSmall)
                }
            }
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
                painter = painterResource(R.drawable.ic_circle_check),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(17.dp),
            )
            Text(text = stringResource(R.string.groups_ready), style = MaterialTheme.typography.labelSmall, color = colors.primary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Shortcut(R.drawable.ic_calendar_days, stringResource(R.string.groups_schedule), onSchedule, Modifier.weight(1f))
            Shortcut(R.drawable.ic_vote, stringResource(R.string.groups_vote), onVote, Modifier.weight(1f))
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
            onNewGroup = {}, onOpenGroup = {}, onInvite = {}, onSchedule = {}, onVote = {},
            onPlaces = {}, onCreateTrip = {}, onJoinGroup = {}, onTabSelect = {},
        )
    }
}

@ThemePreviews
@Composable
private fun TripsEmptyPreview() {
    TripFairTheme {
        GroupsScreen(
            onNewGroup = {}, onOpenGroup = {}, onInvite = {}, onSchedule = {}, onVote = {},
            onPlaces = {}, onCreateTrip = {}, onJoinGroup = {}, onTabSelect = {}, hasGroups = false,
        )
    }
}
