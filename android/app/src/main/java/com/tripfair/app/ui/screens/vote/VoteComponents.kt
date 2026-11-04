package com.tripfair.app.ui.screens.vote

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.ProgressBar
import com.tripfair.app.ui.sample.SampleTrip
import com.tripfair.app.ui.sample.SampleVote
import com.tripfair.app.ui.sample.VoteOption

/** Trip name and members above a vote ("Chuyến nhóm" in Figma). */
@Composable
internal fun VoteTripHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = SampleTrip.NAME,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = SampleVote.MEMBERS_LINE,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Light teal status box with a 24dp icon, e.g. "Đang nhận bình chọn". */
@Composable
internal fun VoteStatusBox(@DrawableRes icon: Int, title: String, text: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primaryContainer, MaterialTheme.shapes.small)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall.copy(lineHeight = 20.sp), color = colors.primary)
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = colors.primary)
        }
    }
}

/** "x/4 người đã gửi phiếu" with the deadline and a progress bar ("Tiến trình nhóm"). */
@Composable
internal fun VoteProgress(sent: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pluralStringResource(R.plurals.vote_progress, SampleVote.MEMBER_COUNT, sent, SampleVote.MEMBER_COUNT),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.vote_deadline, SampleVote.DEADLINE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        ProgressBar(fraction = sent.toFloat() / SampleVote.MEMBER_COUNT, height = 7.dp)
    }
}

/** 120dp card with a square photo, name and price; [footer] sits at the bottom right of the text. */
@Composable
internal fun VoteOptionCard(option: VoteOption, footer: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Image(
            painter = painterResource(option.image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialTheme.shapes.extraSmall),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = option.name, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp))
                Text(text = option.priceLine, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            footer()
        }
    }
}

/** Bold teal "x/4 đồng ý" line. */
@Composable
internal fun AgreedText(count: String, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.vote_agreed, count),
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

/** Muted note above the bottom buttons, then the buttons themselves ("Hành động" in Figma). */
@Composable
internal fun VoteActions(note: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (note != null) {
            Text(text = note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}
