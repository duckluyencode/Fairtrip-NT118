package com.tripfair.app.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.trips.TripDetailDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.OutlineButton
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TopBarTextAction
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.components.formatVnd
import com.tripfair.app.ui.sample.SampleTripPlan
import com.tripfair.app.ui.screens.trip.TripLoadScaffold
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun GroupPlanRoute(
    tripId: Int,
    onBack: () -> Unit,
    onInvite: () -> Unit,
    onEditBudget: () -> Unit,
    onStartVote: () -> Unit,
    onViewResult: () -> Unit,
    viewModel: TripDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(tripId) { viewModel.start(tripId) }
    val trip = viewModel.trip
    if (trip == null) {
        TripLoadScaffold(
            title = stringResource(R.string.groups_title),
            loading = viewModel.loading,
            error = viewModel.error,
            onRetry = viewModel::load,
            onBack = onBack,
        )
        return
    }
    GroupPlanScreen(
        trip = trip,
        onBack = onBack,
        onInvite = onInvite,
        onEditBudget = onEditBudget,
        onStartVote = onStartVote,
        onViewResult = onViewResult,
    )
}

/** "Lập kế hoạch nhóm": the members, their interests, your own budget and the vote kickoff. */
@Composable
fun GroupPlanScreen(
    trip: TripDetailDto,
    onBack: () -> Unit,
    onInvite: () -> Unit,
    onEditBudget: () -> Unit,
    onStartVote: () -> Unit,
    onViewResult: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TripFairTopBar(title = trip.name ?: trip.city, onBack = onBack) {
                TopBarTextAction(text = stringResource(R.string.create_group_invite), onClick = onInvite)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primaryContainer, MaterialTheme.shapes.small)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(painter = painterResource(R.drawable.ic_person_tick), contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (trip.memberCount > 1) {
                        Text(
                            text = pluralStringResource(R.plurals.plan_members_title, trip.memberCount, trip.memberCount),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.primary,
                        )
                        Text(text = stringResource(R.string.plan_members_text), style = MaterialTheme.typography.bodySmall, color = colors.primary)
                    } else {
                        Text(text = stringResource(R.string.plan_alone_title), style = MaterialTheme.typography.bodyLarge, color = colors.primary)
                        Text(
                            text = stringResource(R.string.plan_alone_text, trip.inviteCode.orEmpty()),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primary,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.groups_budgets),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, letterSpacing = 0.sp),
            )
            trip.members.forEachIndexed { index, member ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MemberAvatar(index = index, size = 44.dp, initial = member.fullName.trim().split(" ").lastOrNull()?.take(1))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (member.isMe) stringResource(R.string.member_you, member.fullName) else member.fullName,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = if (member.isOwner) {
                                "${stringResource(R.string.member_leader)} · ${interestsText(member.interests)}"
                            } else {
                                interestsText(member.interests)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    // Only your own budget is shown; everyone set one when joining, but it stays private.
                    MemberBadge(text = member.budget?.let { formatVnd(it) } ?: stringResource(R.string.plan_budget_set), large = true)
                }
            }
            FairnessNote(text = stringResource(R.string.plan_fairness_note))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.plan_vote_deadline), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    text = trip.voteDeadline?.let { deadlineText(it) } ?: stringResource(R.string.plan_vote_deadline_none),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.primary,
                )
            }
            OutlineButton(text = stringResource(R.string.plan_edit_budget), onClick = onEditBudget, trailingIcon = R.drawable.ic_arrow_right)
            if (trip.isPlanned) {
                OutlineButton(text = stringResource(R.string.plan_view_result), onClick = onViewResult, trailingIcon = R.drawable.ic_arrow_right)
            }
            PrimaryButton(text = stringResource(R.string.plan_start_vote), onClick = onStartVote, height = 52.dp)
        }
    }
}

/** "20:00 12/12", in the phone's time zone. */
private fun deadlineText(iso: String): String = try {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm d/M"))
} catch (_: java.time.format.DateTimeParseException) {
    iso
}

@ThemePreviews
@Composable
private fun GroupPlanScreenPreview() {
    TripFairTheme {
        GroupPlanScreen(trip = SampleTripPlan.trip, onBack = {}, onInvite = {}, onEditBudget = {}, onStartVote = {}, onViewResult = {})
    }
}
