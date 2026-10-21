package com.tripfair.app.ui.screens.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tripfair.app.R
import com.tripfair.app.ui.components.AvatarButton
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.QuickFact
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SamplePlace
import com.tripfair.app.ui.sample.SamplePlaces
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun PlaceDetailScreen(
    place: SamplePlace,
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onReviews: () -> Unit,
    onAddToVote: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var isFavorite by rememberSaveable(place.id) { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(R.string.detail_title), onBack = onBack) {
                AvatarButton(onClick = onProfile)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Image(
                painter = painterResource(place.heroImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(285.dp),
            )
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(text = place.name, style = MaterialTheme.typography.headlineMedium)
                        Text(text = place.area, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.tertiaryContainer)
                            .clickable(role = Role.Checkbox, onClickLabel = stringResource(R.string.cd_favorite)) {
                                isFavorite = !isFavorite
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_like),
                            contentDescription = stringResource(R.string.cd_favorite),
                            tint = if (isFavorite) colors.tertiary else colors.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.detail_rating_chip, place.rating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPrimary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.primary)
                            .clickable(role = Role.Button, onClick = onReviews)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.detail_reviews_line, place.reviewCount, place.openingHours),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.clickable(role = Role.Button, onClick = onReviews),
                    )
                }
                Text(text = place.description, style = MaterialTheme.typography.bodyLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    QuickFact(label = place.priceLabel, value = place.price)
                    QuickFact(label = stringResource(R.string.detail_duration), value = place.duration)
                    QuickFact(label = stringResource(R.string.detail_distance), value = place.distanceFromCenter)
                }
                Text(
                    text = place.groupHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.primaryContainer, MaterialTheme.shapes.extraSmall)
                        .padding(12.dp),
                )
                PrimaryButton(
                    text = stringResource(R.string.detail_add_to_vote),
                    onClick = onAddToVote,
                    height = 52.dp,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PlaceDetailScreenPreview() {
    TripFairTheme {
        PlaceDetailScreen(
            place = SamplePlaces.goldenBridge,
            onBack = {},
            onProfile = {},
            onReviews = {},
            onAddToVote = {},
        )
    }
}
