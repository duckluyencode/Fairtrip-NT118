package com.tripfair.app.ui.screens.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.AvatarButton
import com.tripfair.app.ui.components.MainTab
import com.tripfair.app.ui.components.MainTabBar
import com.tripfair.app.ui.components.PlaceCard
import com.tripfair.app.ui.components.SectionHeader
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleData
import com.tripfair.app.ui.sample.SamplePlace
import com.tripfair.app.ui.sample.SamplePlaces
import com.tripfair.app.ui.sample.SampleTrip
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import com.tripfair.app.ui.theme.extendedColors

@Composable
fun HomeScreen(
    onSearch: () -> Unit,
    onSos: () -> Unit,
    onCamera: () -> Unit,
    onTrip: () -> Unit,
    onSeeAllSuggestions: () -> Unit,
    onPlace: (SamplePlace) -> Unit,
    onHistory: () -> Unit,
    onTabSelect: (MainTab) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // Figma sets the home section titles in regular weight, unlike other screens.
    val sectionStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal)

    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(R.string.home_greeting, SampleData.USER_NAME)) {
                AvatarButton(onClick = { onTabSelect(MainTab.Profile) })
            }
        },
        bottomBar = { MainTabBar(selected = MainTab.Explore, onSelect = onTabSelect) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SearchEntry(onClick = onSearch)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(
                    title = stringResource(R.string.home_sos_title),
                    subtitle = stringResource(R.string.home_sos_subtitle),
                    icon = R.drawable.ic_alert_circle,
                    iconTint = colors.error,
                    iconBackground = colors.errorContainer,
                    background = MaterialTheme.extendedColors.errorSurface,
                    border = MaterialTheme.extendedColors.errorBorder,
                    onClick = onSos,
                    modifier = Modifier.weight(1f),
                )
                QuickAction(
                    title = stringResource(R.string.home_camera_title),
                    subtitle = stringResource(R.string.home_camera_subtitle),
                    icon = R.drawable.ic_camera,
                    iconTint = colors.primary,
                    iconBackground = MaterialTheme.extendedColors.primaryIconContainer,
                    background = MaterialTheme.extendedColors.primarySurface,
                    border = MaterialTheme.extendedColors.primaryBorder,
                    onClick = onCamera,
                    modifier = Modifier.weight(1f),
                )
            }
            UpcomingTripCard(onClick = onTrip)
            SectionHeader(
                title = stringResource(R.string.home_group_suggestions),
                titleStyle = sectionStyle,
                actionLabel = stringResource(R.string.home_see_all),
                onAction = onSeeAllSuggestions,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SamplePlaces.groupSuggestions.forEach { place ->
                    PlaceCard(
                        name = place.name,
                        detail = stringResource(R.string.place_card_detail, place.rating, place.categoryLabel),
                        image = place.cardImage,
                        onClick = { onPlace(place) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            SectionHeader(
                title = stringResource(R.string.home_recent),
                titleStyle = sectionStyle,
                actionLabel = stringResource(R.string.action_history),
                onAction = onHistory,
            )
        }
    }
}

/** Looks like a search field but only opens the search screen. */
@Composable
private fun SearchEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.home_search_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    @DrawableRes icon: Int,
    iconTint: Color,
    iconBackground: Color,
    background: Color,
    border: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .border(BorderStroke(1.dp, border), MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UpcomingTripCard(onClick: () -> Unit) {
    val onImage = Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(194.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Image(
            painter = painterResource(R.drawable.sample_trip_da_nang),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.extendedColors.imageScrim),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.home_upcoming_trip, SampleTrip.MEMBER_COUNT),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = onImage,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = SampleTrip.NAME, style = MaterialTheme.typography.headlineMedium, color = onImage)
                Text(text = SampleTrip.DATES, style = MaterialTheme.typography.bodyMedium, color = onImage)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun HomeScreenPreview() {
    TripFairTheme {
        HomeScreen(
            onSearch = {},
            onSos = {},
            onCamera = {},
            onTrip = {},
            onSeeAllSuggestions = {},
            onPlace = {},
            onHistory = {},
            onTabSelect = {},
        )
    }
}
