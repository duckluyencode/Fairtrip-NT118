package com.tripfair.app.ui.screens.vote

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tripfair.app.R
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.ProgressBar
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleVote
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/** "Danh sách bình chọn": the leader's summary of every member's choices. */
@Composable
fun VoteListScreen(onBack: () -> Unit, onFinish: () -> Unit) {
    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.vote_list_title), onBack = onBack) },
        bottomBar = {
            VoteActions(note = stringResource(R.string.vote_leader_note)) {
                PrimaryButton(text = stringResource(R.string.vote_finish), onClick = onFinish, height = 52.dp)
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
            VoteTripHeader()
            VoteStatusBox(
                icon = R.drawable.ic_info,
                title = stringResource(R.string.vote_open_status),
                text = stringResource(R.string.vote_list_summary),
            )
            VoteProgress(sent = SampleVote.SENT_AFTER_YOU)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.vote_places), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    text = pluralStringResource(R.plurals.vote_place_count, SampleVote.options.size, SampleVote.options.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SampleVote.options.forEach { option ->
                VoteOptionCard(option = option) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AgreedText(count = option.agreed)
                        ProgressBar(fraction = option.agreedFraction, height = 5.dp)
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
        VoteListScreen(onBack = {}, onFinish = {})
    }
}
