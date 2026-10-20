package com.tripfair.app.ui.screens.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import com.tripfair.app.ui.components.TopBarTextAction
import com.tripfair.app.ui.components.TripFairChip
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.PlaceCategory
import com.tripfair.app.ui.sample.SamplePlace
import com.tripfair.app.ui.sample.SamplePlaces
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/** Category filter chips; null means "Tất cả". */
private val Filters = listOf(
    null to R.string.filter_all,
    PlaceCategory.Food to R.string.filter_food,
    PlaceCategory.Fun to R.string.filter_fun,
    PlaceCategory.Scenic to R.string.filter_scenic,
)

/** Pin positions on the sample map, as fractions of its width and dp from the top (from Figma). */
private val MapPins = listOf(78f / 354f to 82, 205f / 354f to 46, 272f / 354f to 145)

@Composable
fun SearchResultsScreen(
    query: String,
    onEditQuery: () -> Unit,
    onPlace: (SamplePlace) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf<PlaceCategory?>(null) }
    var favorites by rememberSaveable { mutableStateOf(setOf<String>()) }
    val results = SamplePlaces.searchResults.filter { filter == null || filter in it.categories }

    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(R.string.results_title)) {
                // The map view is not designed yet.
                TopBarTextAction(text = stringResource(R.string.results_map), onClick = {})
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            QueryBox(query = query, onClick = onEditQuery)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Filters.forEach { (category, label) ->
                    TripFairChip(
                        text = stringResource(label),
                        selected = filter == category,
                        onClick = { filter = category },
                    )
                }
            }
            SampleMap()
            Text(
                text = pluralStringResource(R.plurals.results_count, results.size, results.size),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            results.forEach { place ->
                ResultRow(
                    place = place,
                    isFavorite = place.id in favorites,
                    onToggleFavorite = {
                        favorites = if (place.id in favorites) favorites - place.id else favorites + place.id
                    },
                    onClick = { onPlace(place) },
                )
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
        Icon(
            painter = painterResource(R.drawable.ic_filter),
            contentDescription = stringResource(R.string.cd_filter),
            tint = colors.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Static map picture with place pins; replaced by Google Maps SDK later. */
@Composable
private fun SampleMap() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(245.dp)
            .clip(MaterialTheme.shapes.large),
    ) {
        Image(
            painter = painterResource(R.drawable.sample_map_da_nang),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        MapPins.forEach { (x, y) ->
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * x, y = y.dp)
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "★",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ResultRow(
    place: SamplePlace,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(place.thumbImage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 72.dp, height = 64.dp)
                .clip(MaterialTheme.shapes.extraSmall),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = place.name, style = MaterialTheme.typography.titleSmall)
            Text(
                text = place.resultLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (isFavorite) "♥" else "♡",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(role = Role.Checkbox, onClickLabel = stringResource(R.string.cd_favorite), onClick = onToggleFavorite)
                .padding(8.dp),
        )
    }
}

@ThemePreviews
@Composable
private fun SearchResultsScreenPreview() {
    TripFairTheme {
        SearchResultsScreen(query = "Đà Nẵng", onEditQuery = {}, onPlace = {})
    }
}
