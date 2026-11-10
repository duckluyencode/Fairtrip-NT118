package com.tripfair.app.ui.screens.vote

import androidx.annotation.StringRes
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.places.PlaceDetailDto
import com.tripfair.app.data.places.PriceDto
import com.tripfair.app.data.trips.TripSummaryDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.LoadingState
import com.tripfair.app.ui.components.PlacePlaceholder
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.QuickFact
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleTrip
import com.tripfair.app.ui.screens.explore.durationText
import com.tripfair.app.ui.screens.explore.priceLabel
import com.tripfair.app.ui.screens.explore.priceValue
import com.tripfair.app.ui.screens.explore.ratingText
import com.tripfair.app.ui.screens.trip.title
import com.tripfair.app.ui.screens.trip.tripDatesLine
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun AddToVoteRoute(
    placeId: String,
    onBack: () -> Unit,
    onAdded: (tripId: Int) -> Unit,
    viewModel: AddToVoteViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(placeId) { placeId.toIntOrNull()?.let(viewModel::start) }
    LaunchedEffect(viewModel.addedTo) {
        viewModel.addedTo?.let {
            onAdded(it)
            viewModel.onNavigated()
        }
    }
    AddToVoteScreen(
        place = viewModel.place,
        groups = viewModel.groups,
        selectedTripId = viewModel.selectedTripId,
        loading = viewModel.loading,
        saving = viewModel.saving,
        error = viewModel.error,
        onSelect = viewModel::select,
        onBack = onBack,
        onAdd = viewModel::add,
    )
}

/** "Thêm vào bình chọn": puts [place] on the vote list of one of the user's [groups]. */
@Composable
fun AddToVoteScreen(
    place: PlaceDetailDto?,
    groups: List<TripSummaryDto>,
    selectedTripId: Int?,
    loading: Boolean,
    saving: Boolean,
    @StringRes error: Int?,
    onSelect: (tripId: Int) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.add_vote_title), onBack = onBack) },
        bottomBar = {
            VoteActions(note = stringResource(R.string.add_vote_note)) {
                error?.let { Text(text = stringResource(it), style = MaterialTheme.typography.bodySmall, color = colors.error) }
                PrimaryButton(
                    text = stringResource(R.string.add_vote_title),
                    onClick = onAdd,
                    height = 52.dp,
                    enabled = place != null && selectedTripId != null && !saving,
                )
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
            if (loading) {
                LoadingState()
                return@Column
            }
            place?.let { PlaceSummaryCard(place = it) }
            Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.add_vote_choose_trip),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                if (groups.isEmpty()) {
                    Text(text = stringResource(R.string.add_vote_no_group), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                groups.forEach { trip ->
                    GroupTripCard(trip = trip, selected = trip.id == selectedTripId, onSelect = { onSelect(trip.id) })
                }
            }
            place?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable(role = Role.Button, onClick = onBack)
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painter = painterResource(R.drawable.ic_arrow_left), contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.add_vote_back_to_detail, it.name),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary,
                    )
                }
            }
        }
    }
}

/** The place being added: name, area, price with its source, visit time and rating. */
@Composable
private fun PlaceSummaryCard(place: PlaceDetailDto) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(colors.surfaceContainerLow),
    ) {
        PlacePlaceholder(
            category = place.category,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            iconSize = 36,
        )
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = place.name, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp, lineHeight = 31.sp))
                place.area?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            }
            HorizontalDivider(color = colors.outline)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuickFact(label = priceLabel(place.price), value = priceValue(place.price))
                QuickFact(label = stringResource(R.string.detail_duration), value = durationText(place.visitMinutes))
                place.rating?.let { QuickFact(label = stringResource(R.string.add_vote_rating), value = ratingText(it)) }
            }
        }
    }
}

/** One of the user's groups; the selected one gets a teal border and a check. */
@Composable
private fun GroupTripCard(trip: TripSummaryDto, selected: Boolean, onSelect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(colors.surfaceContainerLow)
            .border(1.5.dp, if (selected) colors.primary else Color.Transparent, MaterialTheme.shapes.small)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = trip.title(), style = MaterialTheme.typography.titleSmall.copy(lineHeight = 20.sp))
                Text(text = "${trip.city} · ${tripDatesLine(trip)}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(colors.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(13.dp))
                }
            }
        }
        Text(
            text = pluralStringResource(R.plurals.groups_member_count, trip.memberCount, trip.memberCount),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.primary,
            modifier = Modifier
                .background(colors.primaryContainer, CircleShape)
                .padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@ThemePreviews
@Composable
private fun AddToVoteScreenPreview() {
    TripFairTheme {
        AddToVoteScreen(
            place = PlaceDetailDto(
                id = 9, source = "CATALOG", name = "Cầu Vàng", area = "Bà Nà Hills, Hòa Vang", category = "SCENIC", rating = 4.6,
                visitMinutes = 120, price = PriceDto(900_000, 900_000, source = "PARTNER_API", provider = "Klook", isEstimate = false),
            ),
            groups = SampleTrip.summaries.filter { it.isGroup },
            selectedTripId = 1,
            loading = false,
            saving = false,
            error = null,
            onSelect = {},
            onBack = {},
            onAdd = {},
        )
    }
}
