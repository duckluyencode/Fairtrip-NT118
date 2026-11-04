package com.tripfair.app.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.MainTab
import com.tripfair.app.ui.components.MainTabBar
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairChip
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.PlaceCategory
import com.tripfair.app.ui.sample.SamplePlace
import com.tripfair.app.ui.sample.SamplePlaces
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

private val Filters = listOf(
    null to R.string.filter_all,
    PlaceCategory.Food to R.string.filter_food,
    PlaceCategory.Fun to R.string.filter_fun,
    PlaceCategory.Scenic to R.string.filter_scenic,
)

@Composable
fun FavoritesScreen(
    onPlace: (SamplePlace) -> Unit,
    onExplore: () -> Unit,
    onTabSelect: (MainTab) -> Unit,
    initialFavorites: List<SamplePlace> = SamplePlaces.favorites,
) {
    val colors = MaterialTheme.colorScheme
    var filter by rememberSaveable { mutableStateOf<PlaceCategory?>(null) }
    // Unsaving only changes this screen (Figma: "trái tim → bỏ lưu tại chỗ").
    var removedIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    val saved = initialFavorites.filter { it.id !in removedIds }
    val shown = saved.filter { filter == null || filter in it.categories }

    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(R.string.tab_favorites)) {
                Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
        },
        bottomBar = { MainTabBar(selected = MainTab.Favorites, onSelect = onTabSelect) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = stringResource(R.string.favorites_intro), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (saved.isEmpty()) {
                EmptyFavorites(onExplore = onExplore)
                return@Column
            }
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Filters.forEach { (category, label) ->
                    TripFairChip(text = stringResource(label), selected = filter == category, onClick = { filter = category })
                }
            }
            Text(
                text = pluralStringResource(R.plurals.favorites_count, shown.size, shown.size),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurfaceVariant,
            )
            shown.forEach { place ->
                SavedPlaceCard(
                    place = place,
                    onClick = { onPlace(place) },
                    onUnsave = { removedIds = removedIds + place.id },
                )
            }
            Text(text = stringResource(R.string.favorites_hint), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun SavedPlaceCard(place: SamplePlace, onClick: () -> Unit, onUnsave: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(colors.surfaceContainerLow)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(place.thumbImage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 86.dp, height = 92.dp)
                .clip(MaterialTheme.shapes.extraSmall),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = place.name, style = MaterialTheme.typography.titleSmall)
            Text(text = place.area, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(
                text = stringResource(R.string.place_card_detail, place.rating, place.categoryLabel),
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

@ThemePreviews
@Composable
private fun FavoritesScreenPreview() {
    TripFairTheme {
        FavoritesScreen(onPlace = {}, onExplore = {}, onTabSelect = {})
    }
}

@ThemePreviews
@Composable
private fun FavoritesEmptyPreview() {
    TripFairTheme {
        FavoritesScreen(onPlace = {}, onExplore = {}, onTabSelect = {}, initialFavorites = emptyList())
    }
}
