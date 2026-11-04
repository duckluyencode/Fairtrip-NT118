package com.tripfair.app.ui.screens.vote

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.tripfair.app.R
import com.tripfair.app.ui.components.OutlineButton
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleVote
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/**
 * "Bình chọn": a member ticks the places they want, then sends the ballot ("chưa gửi" →
 * "đã gửi"). "Sửa phiếu của bạn" opens it again. Agreement counts are the sample numbers
 * from Figma for each state.
 */
@Composable
fun BallotScreen(onBack: () -> Unit, onAddPlace: () -> Unit, initiallySent: Boolean = false) {
    var sent by rememberSaveable { mutableStateOf(initiallySent) }
    var picks by rememberSaveable { mutableStateOf(SampleVote.yourPicks) }

    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.ballot_title), onBack = onBack) },
        bottomBar = {
            VoteActions(note = null) {
                OutlineButton(
                    text = stringResource(R.string.ballot_add_place),
                    onClick = onAddPlace,
                    trailingIcon = R.drawable.ic_arrow_right,
                )
                if (sent) {
                    OutlineButton(text = stringResource(R.string.ballot_edit), onClick = { sent = false })
                } else {
                    PrimaryButton(
                        text = pluralStringResource(R.plurals.ballot_send, picks.size, picks.size),
                        onClick = { sent = true },
                        height = 52.dp,
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
            VoteTripHeader()
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
                    text = stringResource(R.string.ballot_open_text, SampleVote.DEADLINE),
                )
            }
            VoteProgress(sent = if (sent) SampleVote.SENT_AFTER_YOU else SampleVote.SENT_BEFORE_YOU)
            Text(text = stringResource(R.string.ballot_options), style = MaterialTheme.typography.titleMedium)
            SampleVote.options.forEach { option ->
                VoteOptionCard(option = option) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        AgreedText(count = if (sent) option.ballotSent else option.ballotOpen, modifier = Modifier.weight(1f))
                        val label = stringResource(R.string.cd_vote_pick, option.name)
                        PickCircle(
                            selected = option.placeId in picks,
                            // A sent ballot is read-only until "Sửa phiếu của bạn".
                            enabled = !sent,
                            onToggle = {
                                picks = if (option.placeId in picks) picks - option.placeId else picks + option.placeId
                            },
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
        BallotScreen(onBack = {}, onAddPlace = {})
    }
}

@ThemePreviews
@Composable
private fun BallotSentPreview() {
    TripFairTheme {
        BallotScreen(onBack = {}, onAddPlace = {}, initiallySent = true)
    }
}
