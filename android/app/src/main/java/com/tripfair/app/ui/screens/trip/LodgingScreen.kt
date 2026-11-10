package com.tripfair.app.ui.screens.trip

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.trips.LodgingOptionDto
import com.tripfair.app.data.trips.TripDetailDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.FillHeightScrollColumn
import com.tripfair.app.ui.components.LoadingState
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.components.formatVnd
import com.tripfair.app.ui.screens.explore.distanceText
import com.tripfair.app.ui.screens.explore.ratingText
import com.tripfair.app.ui.sample.SampleLodging
import com.tripfair.app.ui.screens.group.IconTile
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import java.time.LocalDate

@Composable
fun LodgingRoute(
    tripId: Int,
    onBack: () -> Unit,
    onConfirmed: () -> Unit,
    viewModel: LodgingViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(tripId) { viewModel.start(tripId) }
    LaunchedEffect(viewModel.done) {
        if (viewModel.done) {
            onConfirmed()
            viewModel.onNavigated()
        }
    }
    LodgingScreen(
        trip = viewModel.trip,
        options = viewModel.options,
        fetchedAt = viewModel.fetchedAt,
        selected = viewModel.selected,
        loading = viewModel.loading,
        saving = viewModel.saving,
        error = viewModel.error,
        onSelect = viewModel::select,
        onBack = onBack,
        onConfirm = viewModel::confirm,
    )
}

/**
 * "Chọn chỗ ở": up to three hotels that fit the budget, or "Tự lo chỗ ở". The room price is
 * taken from the budget first; each card shows what is left for the places.
 */
@Composable
fun LodgingScreen(
    trip: TripDetailDto?,
    options: List<LodgingOptionDto>,
    fetchedAt: String,
    selected: Int,
    loading: Boolean,
    saving: Boolean,
    @StringRes error: Int?,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val hotel = options.firstOrNull { it.placeId == selected }

    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.lodging_title), onBack = onBack) },
    ) { innerPadding ->
        FillHeightScrollColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
        ) {
            Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    trip?.let {
                        val range = tripDateRange(LocalDate.parse(it.startDate), LocalDate.parse(it.endDate))
                        Text(
                            text = stringResource(R.string.lodging_trip_line, it.city, range, it.days - 1, options.firstOrNull()?.guestsPerRoom ?: 2),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primary,
                        )
                    }
                    Text(
                        text = stringResource(R.string.lodging_heading),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, lineHeight = 29.sp),
                    )
                    if (fetchedAt.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.lodging_intro, fetchedAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                if (loading) {
                    LoadingState()
                } else {
                    if (options.isEmpty()) {
                        Text(text = stringResource(R.string.lodging_none), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    options.forEach { item ->
                        HotelCard(hotel = item, selected = item.placeId == selected, onSelect = { onSelect(item.placeId) })
                    }
                    SelfArrangedCard(selected = hotel == null, onSelect = { onSelect(SELF_LODGING) })
                }
            }
            Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.lodging_price_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
                error?.let { Text(text = stringResource(it), style = MaterialTheme.typography.bodySmall, color = colors.error) }
                PrimaryButton(
                    text = if (hotel != null) {
                        stringResource(R.string.lodging_confirm, hotel.name)
                    } else {
                        stringResource(R.string.lodging_confirm_self)
                    },
                    onClick = onConfirm,
                    trailingIcon = R.drawable.ic_arrow_right,
                    height = 52.dp,
                    enabled = !loading && !saving,
                )
            }
        }
    }
}

/** Hotel option; the selected one gets a 2dp teal border, and the balanced one a filled tag. */
@Composable
private fun HotelCard(hotel: LodgingOptionDto, selected: Boolean, onSelect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val recommended = hotel.tag == "BALANCED"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surfaceContainerLow)
            .border(2.dp, if (selected) colors.primary else Color.Transparent, MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon = R.drawable.ic_key_round, size = 64.dp, iconSize = 28.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = stringResource(
                    when (hotel.tag) {
                        "CHEAPEST" -> R.string.lodging_tag_cheapest
                        "NEAREST" -> R.string.lodging_tag_nearest
                        else -> R.string.lodging_tag_balanced
                    },
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold),
                color = if (recommended) colors.onPrimary else colors.primary,
                modifier = Modifier
                    .background(if (recommended) colors.primary else colors.primaryContainer, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Text(text = hotel.name, style = MaterialTheme.typography.labelLarge.copy(lineHeight = 19.sp))
            val rating = hotel.rating?.let { ratingText(it) }
            val distance = hotel.distanceKm?.let { distanceText(it) }
            val place = when {
                rating != null && distance != null -> stringResource(R.string.lodging_rating, rating, distance)
                rating != null -> stringResource(R.string.lodging_rating_only, rating)
                distance != null -> stringResource(R.string.lodging_distance_only, distance)
                else -> null
            }
            place?.let { Text(text = it, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant) }
            val price = formatVnd(hotel.pricePerNight).let { if (hotel.isEstimate) "~$it" else it }
            Text(
                text = pluralStringResource(R.plurals.lodging_price, hotel.guestsPerRoom, price, hotel.guestsPerRoom),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = priceSourceLabel(hotel.priceSource, hotel.provider),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.lodging_share, formatVnd(hotel.myShare), formatVnd(hotel.leftForPlaces)),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primary,
            )
        }
    }
}

/** "Tự lo chỗ ở": no room cost, the whole budget goes to the places. */
@Composable
private fun SelfArrangedCard(selected: Boolean, onSelect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surfaceContainerLow)
            .border(2.dp, if (selected) colors.primary else Color.Transparent, MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioDot(selected = selected)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = stringResource(R.string.lodging_self_title), style = MaterialTheme.typography.titleSmall)
            Text(text = stringResource(R.string.lodging_self_text), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

/** 20dp radio circle; filled teal centre when selected. */
@Composable
private fun RadioDot(selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(1.5.dp, if (selected) colors.primary else colors.onSurfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(colors.primary, CircleShape),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun LodgingScreenPreview() {
    TripFairTheme {
        LodgingScreen(
            trip = null,
            options = SampleLodging.options,
            fetchedAt = "09:35",
            selected = SampleLodging.options.first().placeId,
            loading = false,
            saving = false,
            error = null,
            onSelect = {},
            onBack = {},
            onConfirm = {},
        )
    }
}
