package com.tripfair.app.ui.screens.vote

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.trips.TripDetailDto
import com.tripfair.app.data.trips.VoteOptionListDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.EmptyState
import com.tripfair.app.ui.components.OutlineButton
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.ProgressBar
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleTripPlan
import com.tripfair.app.ui.sample.SampleVote
import com.tripfair.app.ui.screens.trip.TripLoadScaffold
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun VoteListRoute(
    tripId: Int,
    onBack: () -> Unit,
    onBallot: () -> Unit,
    onFinish: (wantsHotel: Boolean) -> Unit,
    viewModel: VotesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(tripId) { viewModel.start(tripId) }
    val trip = viewModel.trip
    val votes = viewModel.votes
    if (trip == null || votes == null) {
        TripLoadScaffold(
            title = stringResource(R.string.vote_list_title),
            loading = viewModel.loading,
            error = viewModel.error,
            onRetry = viewModel::load,
            onBack = onBack,
        )
        return
    }
    VoteListScreen(
        trip = trip,
        votes = votes,
        error = viewModel.error,
        onBack = onBack,
        onBallot = onBallot,
        onFinish = { onFinish(trip.lodgingMode == "HOTEL" && trip.days > 1) },
    )
}

/**
 * "Danh sách bình chọn": every member's choices so far. Anyone can open their own ballot
 * from here; only the leader can end the vote and start planning.
 */
@Composable
fun VoteListScreen(
    trip: TripDetailDto,
    votes: VoteOptionListDto,
    @StringRes error: Int?,
    onBack: () -> Unit,
    onBallot: () -> Unit,
    onFinish: () -> Unit,
) {
    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.vote_list_title), onBack = onBack) },
        bottomBar = {
            VoteActions(note = stringResource(R.string.vote_leader_note)) {
                OutlineButton(text = stringResource(R.string.vote_my_ballot), onClick = onBallot, trailingIcon = R.drawable.ic_arrow_right)
                if (trip.isOwner) {
                    PrimaryButton(text = stringResource(R.string.vote_finish), onClick = onFinish, height = 52.dp)
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
            VoteStatusBox(
                icon = R.drawable.ic_info,
                title = stringResource(R.string.vote_open_status),
                text = stringResource(R.string.vote_list_summary),
            )
            VoteProgress(sent = votes.ballotsSent, memberCount = votes.memberCount, deadline = trip.voteDeadline)
            error?.let { Text(text = stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.vote_places), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    text = pluralStringResource(R.plurals.vote_place_count, votes.items.size, votes.items.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (votes.items.isEmpty()) EmptyState(text = stringResource(R.string.vote_empty))
            votes.items.forEach { option ->
                VoteOptionCard(option = option) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AgreedText(count = "${option.votes}/${votes.memberCount}")
                        ProgressBar(
                            fraction = if (votes.memberCount > 0) option.votes.toFloat() / votes.memberCount else 0f,
                            height = 5.dp,
                        )
                    }
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun VoteListScreenPreview() {
    TripFairTheme {
        VoteListScreen(trip = SampleTripPlan.trip, votes = SampleVote.votes, error = null, onBack = {}, onBallot = {}, onFinish = {})
    }
}
