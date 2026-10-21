package com.tripfair.app.ui.screens.vote

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.QuickFact
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleGroup
import com.tripfair.app.ui.sample.SamplePlace
import com.tripfair.app.ui.sample.SamplePlaces
import com.tripfair.app.ui.sample.SampleTrip
import com.tripfair.app.ui.screens.group.MemberAvatar
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/** "Thêm vào bình chọn": puts [place] on the group's vote list after picking the trip. */
@Composable
fun AddToVoteScreen(
    place: SamplePlace,
    onBack: () -> Unit,
    onBackToDetail: () -> Unit,
    onAdd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.add_vote_title), onBack = onBack) },
        bottomBar = {
            VoteActions(note = stringResource(R.string.add_vote_note)) {
                PrimaryButton(text = stringResource(R.string.add_vote_title), onClick = onAdd, height = 52.dp)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(colors.surfaceContainerLow),
            ) {
                Image(
                    painter = painterResource(place.heroImage),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(158.dp),
                )
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(text = place.name, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp, lineHeight = 31.sp))
                        Text(text = place.area, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    HorizontalDivider(color = colors.outline)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        QuickFact(label = place.pricePerPersonLabel, value = place.price)
                        QuickFact(label = stringResource(R.string.detail_duration), value = place.duration)
                        QuickFact(label = stringResource(R.string.detail_distance), value = place.distanceFromCenter)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primaryContainer, MaterialTheme.shapes.extraSmall)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(painter = painterResource(R.drawable.ic_info), contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                Text(text = place.groupHint, style = MaterialTheme.typography.bodySmall, color = colors.primary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.add_vote_choose_trip),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                SelectedTripCard()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable(role = Role.Button, onClick = onBackToDetail)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painter = painterResource(R.drawable.ic_arrow_left), contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                Text(
                    text = stringResource(R.string.add_vote_back_to_detail, place.name),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primary,
                )
            }
        }
    }
}

/** The group trip that is collecting votes; it is the only one, so it is always selected. */
@Composable
private fun SelectedTripCard() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
            .border(1.5.dp, colors.primary, MaterialTheme.shapes.small)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = SampleTrip.NAME, style = MaterialTheme.typography.titleSmall.copy(lineHeight = 20.sp))
                Text(text = SampleTrip.DATES, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(colors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(13.dp))
            }
        }
        Text(
            text = stringResource(R.string.vote_open_status),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.primary,
            modifier = Modifier
                .background(colors.primaryContainer, CircleShape)
                .padding(horizontal = 9.dp, vertical = 5.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SampleGroup.members.forEachIndexed { index, member ->
                MemberAvatar(index = index, size = 24.dp, initial = member.name.take(1))
            }
            Text(
                text = SampleGroup.members.joinToString { it.name },
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun AddToVoteScreenPreview() {
    TripFairTheme {
        AddToVoteScreen(place = SamplePlaces.goldenBridge, onBack = {}, onBackToDetail = {}, onAdd = {})
    }
}
