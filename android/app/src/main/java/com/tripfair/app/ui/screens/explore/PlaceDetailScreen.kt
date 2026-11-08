package com.tripfair.app.ui.screens.explore

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.places.OpeningHoursDto
import com.tripfair.app.data.places.PlaceDetailDto
import com.tripfair.app.data.places.PriceDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.AvatarButton
import com.tripfair.app.ui.components.ErrorState
import com.tripfair.app.ui.components.LoadingState
import com.tripfair.app.ui.components.PlacePlaceholder
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.QuickFact
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun PlaceDetailRoute(
    placeId: String,
    sessionToken: String?,
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onAddToVote: () -> Unit,
    viewModel: PlaceDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(placeId) { viewModel.start(placeId, sessionToken) }
    val uriHandler = LocalUriHandler.current
    PlaceDetailScreen(
        state = viewModel.state,
        onBack = onBack,
        onProfile = onProfile,
        onRetry = viewModel::retry,
        onToggleFavorite = viewModel::toggleFavorite,
        onOpenMaps = { uri -> uriHandler.openUri(uri) },
        onAddToVote = onAddToVote,
    )
}

/** "Chi tiết": one of the screens where prices are shown, always with their source. */
@Composable
fun PlaceDetailScreen(
    state: PlaceDetailUiState,
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenMaps: (String) -> Unit,
    onAddToVote: () -> Unit,
) {
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
            val place = state.place
            when {
                state.loading -> LoadingState()
                place == null -> ErrorState(
                    message = stringResource(state.error ?: R.string.error_unexpected),
                    onRetry = onRetry,
                )
                else -> PlaceContent(place, onToggleFavorite, onOpenMaps, onAddToVote)
            }
        }
    }
}

@Composable
private fun PlaceContent(
    place: PlaceDetailDto,
    onToggleFavorite: () -> Unit,
    onOpenMaps: (String) -> Unit,
    onAddToVote: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    PlacePlaceholder(
        category = place.category,
        iconSize = 56,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
    )
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(text = place.name, style = MaterialTheme.typography.headlineMedium)
                place.area?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.tertiaryContainer)
                    .clickable(role = Role.Checkbox, onClickLabel = stringResource(R.string.cd_favorite), onClick = onToggleFavorite),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (place.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart),
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (place.isFavorite) colors.tertiary else colors.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (place.rating != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.detail_rating_chip, ratingText(place.rating)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.primary)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Text(
                    text = stringResource(
                        R.string.detail_reviews_line,
                        countText(place.ratingCount ?: 0),
                        hoursText(place.openingHours),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        place.description?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            QuickFact(label = priceLabel(place.price), value = priceValue(place.price))
            QuickFact(label = stringResource(R.string.detail_duration), value = durationText(place.visitMinutes))
            QuickFact(label = stringResource(R.string.detail_hours), value = hoursText(place.openingHours))
        }
        if (place.mapsUri != null || place.attribution != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Google's terms: data from Google Maps is credited where it is shown.
                place.attribution?.let {
                    Text(
                        text = stringResource(R.string.detail_source, it),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                place.mapsUri?.let { uri ->
                    TextButton(onClick = { onOpenMaps(uri) }) {
                        Text(text = stringResource(R.string.detail_open_maps), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        PrimaryButton(
            text = stringResource(R.string.detail_add_to_vote),
            onClick = onAddToVote,
            height = 52.dp,
        )
    }
}

@ThemePreviews
@Composable
private fun PlaceDetailScreenPreview() {
    TripFairTheme {
        PlaceDetailScreen(
            state = PlaceDetailUiState(
                loading = false,
                place = PlaceDetailDto(
                    id = 1,
                    source = "GOOGLE",
                    name = "Mì Quảng Bà Mua",
                    area = "19 Trần Bình Trọng, Hải Châu, Đà Nẵng",
                    category = "FOOD",
                    rating = 4.4,
                    ratingCount = 3200,
                    attribution = "Google Maps",
                    description = "Quán mì Quảng lâu năm ở trung tâm Đà Nẵng.",
                    visitMinutes = 60,
                    openingHours = OpeningHoursDto(allDay = false, opensAt = "06:00", closesAt = "21:00"),
                    price = PriceDto(50_000, 150_000, source = "ESTIMATED", provider = "TripFair", isEstimate = true),
                    mapsUri = "https://maps.google.com/?cid=1",
                ),
            ),
            onBack = {},
            onProfile = {},
            onRetry = {},
            onToggleFavorite = {},
            onOpenMaps = {},
            onAddToVote = {},
        )
    }
}
