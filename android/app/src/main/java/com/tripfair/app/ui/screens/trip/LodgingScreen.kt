package com.tripfair.app.ui.screens.trip

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.FillHeightScrollColumn
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.components.formatVnd
import com.tripfair.app.ui.sample.SampleHotel
import com.tripfair.app.ui.sample.SampleItinerary
import com.tripfair.app.ui.sample.SampleLodging
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/** Value of the "Tự lo chỗ ở" choice. */
private const val SELF_ARRANGED = "self"

/**
 * "Chọn chỗ ở": three Agoda hotels that fit the budget, or "Tự lo chỗ ở". The room price is
 * taken from the budget first; the card shows what is left for the places.
 */
@Composable
fun LodgingScreen(onBack: () -> Unit, onConfirm: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(SampleLodging.hotels.first().id) }
    val hotel = SampleLodging.hotels.firstOrNull { it.id == selected }

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
                    Text(
                        text = SampleLodging.TRIP_LINE,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.lodging_heading),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, lineHeight = 29.sp),
                    )
                    Text(
                        text = stringResource(R.string.lodging_intro, SampleLodging.FETCHED_AT),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SampleLodging.hotels.forEachIndexed { index, item ->
                    HotelCard(hotel = item, recommended = index == 0, selected = item.id == selected, onSelect = { selected = item.id })
                }
                SelfArrangedCard(selected = selected == SELF_ARRANGED, onSelect = { selected = SELF_ARRANGED })
            }
            Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.lodging_price_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    text = if (hotel != null) {
                        stringResource(R.string.lodging_confirm, hotel.shortName)
                    } else {
                        stringResource(R.string.lodging_confirm_self)
                    },
                    onClick = onConfirm,
                    trailingIcon = R.drawable.ic_arrow_right,
                    height = 52.dp,
                )
            }
        }
    }
}

/** Hotel option; the selected one gets a 2dp teal border. [recommended] fills the tag in teal. */
@Composable
private fun HotelCard(hotel: SampleHotel, recommended: Boolean, selected: Boolean, onSelect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val share = SampleLodging.yourShare(hotel)
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
        Image(
            painter = painterResource(hotel.image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 84.dp, height = 96.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = hotel.tag,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold),
                color = if (recommended) colors.onPrimary else colors.primary,
                modifier = Modifier
                    .background(if (recommended) colors.primary else colors.primaryContainer, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Text(text = hotel.name, style = MaterialTheme.typography.labelLarge.copy(lineHeight = 19.sp))
            Text(
                text = stringResource(R.string.lodging_rating, hotel.rating, hotel.distanceToRoute),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.lodging_price,
                    SampleLodging.PEOPLE_PER_ROOM,
                    formatVnd(hotel.pricePerNight),
                    SampleLodging.PEOPLE_PER_ROOM,
                ),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = stringResource(R.string.lodging_share, formatVnd(share), formatVnd(SampleItinerary.BUDGET - share)),
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
        LodgingScreen(onBack = {}, onConfirm = {})
    }
}
