package com.tripfair.app.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.places.PlaceSummaryDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.EmptyState
import com.tripfair.app.ui.components.ErrorState
import com.tripfair.app.ui.components.LoadingState
import com.tripfair.app.ui.components.PlacePlaceholder
import com.tripfair.app.ui.components.TripFairChip
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/** Category filter chips: null is "Tất cả". */
private val Filters = listOf(
    null to R.string.filter_all,
    "FOOD" to R.string.filter_food,
    "FUN" to R.string.filter_fun,
    "SCENIC" to R.string.filter_scenic,
)

@Composable
fun SearchResultsRoute(
    query: String,
    onEditQuery: () -> Unit,
    onPlace: (placeId: Int) -> Unit,
    viewModel: SearchResultsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(query) { viewModel.start(query) }
    SearchResultsScreen(
        state = viewModel.state,
        onEditQuery = onEditQuery,
        onCategoryChange = viewModel::onCategoryChange,
        onRetry = viewModel::reload,
        onLoadMore = viewModel::loadMore,
        onToggleFavorite = viewModel::toggleFavorite,
        onPlace = onPlace,
    )
}

/** Search results; lists never show prices. The map view comes with the Google Maps SDK. */
@Composable
fun SearchResultsScreen(
    state: SearchResultsUiState,
    onEditQuery: () -> Unit,
    onCategoryChange: (String?) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onPlace: (Int) -> Unit,
) {
    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.results_title)) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { QueryBox(query = state.query, onClick = onEditQuery) }
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Filters.forEach { (category, label) ->
                        TripFairChip(
                            text = stringResource(label),
                            selected = state.category == category,
                            onClick = { onCategoryChange(category) },
                        )
                    }
                }
            }
            when {
                state.loading -> item { LoadingState() }
                state.error != null -> item { ErrorState(message = stringResource(state.error), onRetry = onRetry) }
                state.items.isEmpty() -> item { EmptyState(text = stringResource(R.string.results_empty)) }
                else -> {
                    items(state.items, key = { it.id }) { place ->
                        ResultRow(
                            place = place,
                            onToggleFavorite = { onToggleFavorite(place.id) },
                            onClick = { onPlace(place.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    if (state.next != null) {
                        item {
                            if (state.loadingMore) {
                                LoadingState()
                            } else {
                                TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = stringResource(R.string.results_load_more), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    state.items.firstNotNullOfOrNull { it.attribution }?.let { source ->
                        item {
                            Text(
                                text = stringResource(R.string.detail_source, source),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Shows the current query; tapping it goes back to editing ("Ô tìm kiếm" in Figma). */
@Composable
private fun QueryBox(query: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(colors.surfaceContainerLow)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Text(text = query, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ResultRow(
    place: PlaceSummaryDto,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = listOfNotNull(
        stringResource(categoryLabel(place.category)),
        place.rating?.let { "${ratingText(it)} ★" },
        place.distanceKm?.let(::distanceText),
    ).joinToString(" · ")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlacePlaceholder(
            category = place.category,
            modifier = Modifier
                .size(width = 72.dp, height = 64.dp)
                .clip(MaterialTheme.shapes.extraSmall),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = place.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            place.area?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (place.isFavorite) "♥" else "♡",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(role = Role.Checkbox, onClickLabel = stringResource(R.string.cd_favorite), onClick = onToggleFavorite)
                .padding(8.dp),
        )
    }
}

private val PreviewPlaces = listOf(
    PlaceSummaryDto(1, "GOOGLE", "Cầu Rồng", "Nguyễn Văn Linh, Đà Nẵng", category = "SCENIC", rating = 4.7, attribution = "Google Maps"),
    PlaceSummaryDto(2, "GOOGLE", "Mì Quảng Bà Mua", "19 Trần Bình Trọng, Đà Nẵng", category = "FOOD", rating = 4.4, isFavorite = true),
)

@ThemePreviews
@Composable
private fun SearchResultsScreenPreview() {
    TripFairTheme {
        SearchResultsScreen(
            state = SearchResultsUiState(query = "Đà Nẵng", items = PreviewPlaces),
            onEditQuery = {},
            onCategoryChange = {},
            onRetry = {},
            onLoadMore = {},
            onToggleFavorite = {},
            onPlace = {},
        )
    }
}
