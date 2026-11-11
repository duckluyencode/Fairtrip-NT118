package com.tripfair.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.places.PlaceSummaryDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.ErrorState
import com.tripfair.app.ui.components.LoadingState
import com.tripfair.app.ui.components.MainTab
import com.tripfair.app.ui.components.MainTabBar
import com.tripfair.app.ui.components.PlacePlaceholder
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairChip
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.screens.explore.categoryLabel
import com.tripfair.app.ui.screens.explore.ratingText
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

private val Filters = listOf(
    null to R.string.filter_all,
    "FOOD" to R.string.filter_food,
    "FUN" to R.string.filter_fun,
    "SCENIC" to R.string.filter_scenic,
)

@Composable
fun FavoritesRoute(
    onPlace: (placeId: Int) -> Unit,
    onExplore: () -> Unit,
    onTabSelect: (MainTab) -> Unit,
    viewModel: FavoritesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    FavoritesScreen(
        state = viewModel.state,
        onCategoryChange = viewModel::onCategoryChange,
        onUnsave = viewModel::unsave,
        onRetry = viewModel::load,
        onPlace = onPlace,
        onExplore = onExplore,
        onTabSelect = onTabSelect,
    )
}

@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    onCategoryChange: (String?) -> Unit,
    onUnsave: (Int) -> Unit,
    onRetry: () -> Unit,
    onPlace: (Int) -> Unit,
    onExplore: () -> Unit,
    onTabSelect: (MainTab) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shown = state.shown

    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(R.string.tab_favorites)) {
                Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
        },
        bottomBar = { MainTabBar(selected = MainTab.Favorites, onSelect = onTabSelect) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(text = stringResource(R.string.favorites_intro), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            when {
                state.loading -> item { LoadingState() }
                state.error != null -> item { ErrorState(message = stringResource(state.error), onRetry = onRetry) }
                state.items.isEmpty() -> item { EmptyFavorites(onExplore = onExplore) }
                else -> {
                    item {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Filters.forEach { (category, label) ->
                                TripFairChip(
                                    text = stringResource(label),
                                    selected = state.category == category,
                                    onClick = { onCategoryChange(category) },
                                )
                            }
                        }
                    }
                    item {
                        Text(
                            text = pluralStringResource(R.plurals.favorites_count, shown.size, shown.size),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.onSurfaceVariant,
                        )
                    }
                    items(shown, key = { it.id }) { place ->
                        SavedPlaceCard(
                            place = place,
                            onClick = { onPlace(place.id) },
                            onUnsave = { onUnsave(place.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item {
                        Text(text = stringResource(R.string.favorites_hint), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedPlaceCard(place: PlaceSummaryDto, onClick: () -> Unit, onUnsave: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val category = stringResource(categoryLabel(place.category))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(colors.surfaceContainerLow)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlacePlaceholder(
            category = place.category,
            modifier = Modifier
                .size(width = 86.dp, height = 92.dp)
                .clip(MaterialTheme.shapes.extraSmall),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = place.name, style = MaterialTheme.typography.titleSmall)
            place.area?.let { Text(text = it, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant) }
            Text(
                text = place.rating?.let { stringResource(R.string.place_card_detail, ratingText(it), category) } ?: category,
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
            )
        }
        IconButton(onClick = onUnsave) {
            Icon(
                painter = painterResource(R.drawable.ic_heart_saved),
                contentDescription = stringResource(R.string.cd_unsave),
                tint = colors.tertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Shown when nothing is saved ("Yêu thích · chưa có" in Figma). */
@Composable
private fun EmptyFavorites(onExplore: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(colors.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(R.drawable.ic_heart_large), contentDescription = null, tint = colors.primary, modifier = Modifier.size(40.dp))
        }
        Text(
            text = stringResource(R.string.favorites_empty_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, letterSpacing = 0.sp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.favorites_empty_text),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            text = stringResource(R.string.favorites_empty_action),
            onClick = onExplore,
            trailingIcon = R.drawable.ic_arrow_right,
            height = 52.dp,
        )
    }
}

private val PreviewFavorites = listOf(
    PlaceSummaryDto(1, "GOOGLE", "Cầu Rồng", "Nguyễn Văn Linh, Đà Nẵng", category = "SCENIC", categories = listOf("SCENIC"), rating = 4.7),
    PlaceSummaryDto(2, "GOOGLE", "Mì Quảng Bà Mua", "Hải Châu, Đà Nẵng", category = "FOOD", categories = listOf("FOOD"), rating = 4.4),
)

@ThemePreviews
@Composable
private fun FavoritesScreenPreview() {
    TripFairTheme {
        FavoritesScreen(
            state = FavoritesUiState(items = PreviewFavorites, loading = false),
            onCategoryChange = {},
            onUnsave = {},
            onRetry = {},
            onPlace = {},
            onExplore = {},
            onTabSelect = {},
        )
    }
}

@ThemePreviews
@Composable
private fun FavoritesEmptyPreview() {
    TripFairTheme {
        FavoritesScreen(
            state = FavoritesUiState(loading = false),
            onCategoryChange = {},
            onUnsave = {},
            onRetry = {},
            onPlace = {},
            onExplore = {},
            onTabSelect = {},
        )
    }
}
