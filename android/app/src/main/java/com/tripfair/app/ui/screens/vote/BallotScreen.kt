package com.tripfair.app.ui.screens.vote

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.trips.TripDetailDto
import com.tripfair.app.data.trips.VoteOptionDto
import com.tripfair.app.data.trips.VoteOptionListDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.EmptyState
import com.tripfair.app.ui.components.OutlineButton
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleTripPlan
import com.tripfair.app.ui.sample.SampleVote
import com.tripfair.app.ui.screens.trip.TripLoadScaffold
import com.tripfair.app.ui.screens.trip.voteDeadlineText
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun BallotRoute(
    tripId: Int,
    onBack: () -> Unit,
    onAddPlace: () -> Unit,
    viewModel: VotesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(tripId) { viewModel.start(tripId) }
    val trip = viewModel.trip
    val votes = viewModel.votes
    if (trip == null || votes == null) {
        TripLoadScaffold(
            title = stringResource(R.string.ballot_title),
            loading = viewModel.loading,
            error = viewModel.error,
            onRetry = viewModel::load,
            onBack = onBack,
        )
        return
    }
    BallotScreen(
        trip = trip,
        votes = votes,
        busy = viewModel.busy,
        error = viewModel.error,
        onBack = onBack,
        onAddPlace = onAddPlace,
        onToggle = viewModel::toggle,
        onSend = viewModel::send,
    )
}

/**
 * "Bình chọn": a member ticks the places they want (each tick is saved), then sends the
 * ballot. A sent ballot can no longer be changed.
 */
@Composable
fun BallotScreen(
    trip: TripDetailDto,
    votes: VoteOptionListDto,
    busy: Boolean,
    @StringRes error: Int?,
    onBack: () -> Unit,
    onAddPlace: () -> Unit,
    onToggle: (VoteOptionDto) -> Unit,
    onSend: () -> Unit,
) {
    val sent = votes.mySentAt != null
    val picked = votes.items.count { it.votedByMe }

    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.ballot_title), onBack = onBack) },
        bottomBar = {
            VoteActions(note = null) {
                error?.let { Text(text = stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                OutlineButton(
                    text = stringResource(R.string.ballot_add_place),
                    onClick = onAddPlace,
                    trailingIcon = R.drawable.ic_arrow_right,
                )
                if (!sent) {
                    PrimaryButton(
                        text = pluralStringResource(R.plurals.ballot_send, picked, picked),
                        onClick = onSend,
                        height = 52.dp,
                        enabled = !busy,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VoteTripHeader(trip = trip)
            if (sent) {
                VoteStatusBox(
                    icon = R.drawable.ic_circle_check,
                    title = stringResource(R.string.ballot_sent_title),
                    text = stringResource(R.string.ballot_sent_text),
                )
            } else {
                VoteStatusBox(
                    icon = R.drawable.ic_circle_check,
                    title = stringResource(R.string.ballot_open_title),
                    text = trip.voteDeadline?.let { stringResource(R.string.ballot_open_text, voteDeadlineText(it)) }
                        ?: stringResource(R.string.ballot_open_text_none),
                )
            }
            VoteProgress(sent = votes.ballotsSent, memberCount = votes.memberCount, deadline = trip.voteDeadline)
            Text(text = stringResource(R.string.ballot_options), style = MaterialTheme.typography.titleMedium)
            if (votes.items.isEmpty()) EmptyState(text = stringResource(R.string.vote_empty))
            votes.items.forEach { option ->
                VoteOptionCard(option = option) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        AgreedText(count = "${option.votes}/${votes.memberCount}", modifier = Modifier.weight(1f))
                        val label = stringResource(R.string.cd_vote_pick, option.name.orEmpty())
                        PickCircle(
                            selected = option.votedByMe,
                            // A sent ballot is read-only.
                            enabled = !sent && !busy,
                            onToggle = { onToggle(option) },
                            modifier = Modifier.semantics { contentDescription = label },
                        )
                    }
                }
            }
        }
    }
}

/** 36dp choice circle: teal ring with a check when picked, grey ring when not. */
@Composable
private fun PickCircle(selected: Boolean, enabled: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (selected) colors.primaryContainer else Color.Transparent)
            .border(
                width = if (selected) 2.dp else 1.5.dp,
                color = if (selected) colors.primary else colors.onSurfaceVariant,
                shape = CircleShape,
            )
            .toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun BallotScreenPreview() {
    TripFairTheme {
        BallotScreen(
            trip = SampleTripPlan.trip, votes = SampleVote.votes, busy = false, error = null,
            onBack = {}, onAddPlace = {}, onToggle = {}, onSend = {},
        )
    }
}

@ThemePreviews
@Composable
private fun BallotSentPreview() {
    TripFairTheme {
        BallotScreen(
            trip = SampleTripPlan.trip, votes = SampleVote.votes.copy(ballotsSent = 3, mySentAt = "2026-12-01T10:00:00.000Z"),
            busy = false, error = null, onBack = {}, onAddPlace = {}, onToggle = {}, onSend = {},
        )
    }
}
