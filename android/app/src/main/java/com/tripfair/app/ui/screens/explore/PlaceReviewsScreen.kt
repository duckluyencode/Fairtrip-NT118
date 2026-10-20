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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.ChipStyle
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairChip
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SamplePlace
import com.tripfair.app.ui.sample.SamplePlaces
import com.tripfair.app.ui.sample.SampleReview
import com.tripfair.app.ui.sample.SampleSearch
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import com.tripfair.app.ui.theme.extendedColors

private enum class ReviewFilter(val label: Int) {
    All(R.string.filter_all),
    FiveStars(R.string.reviews_filter_5),
    FourStars(R.string.reviews_filter_4),
    WithPhoto(R.string.reviews_filter_photo),
}

@Composable
fun PlaceReviewsScreen(
    place: SamplePlace,
    onBack: () -> Unit,
    onWriteReview: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf(ReviewFilter.All) }
    val reviews = SampleSearch.reviews.filter {
        when (filter) {
            ReviewFilter.All -> true
            ReviewFilter.FiveStars -> it.stars == 5
            ReviewFilter.FourStars -> it.stars == 4
            ReviewFilter.WithPhoto -> it.hasPhoto
        }
    }

    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.reviews_title), onBack = onBack) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                PrimaryButton(text = stringResource(R.string.reviews_write), onClick = onWriteReview, height = 52.dp)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RatingSummary(place = place)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.reviews_section),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f),
                    )
                    // Sorting options are not designed yet; only the default order exists.
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = stringResource(R.string.reviews_sort_newest),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_down),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReviewFilter.entries.forEach { option ->
                        TripFairChip(
                            text = stringResource(option.label),
                            selected = filter == option,
                            onClick = { filter = option },
                            style = ChipStyle.Soft,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.reviews_source_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                reviews.forEach { ReviewCard(review = it) }
            }
        }
    }
}

@Composable
private fun RatingSummary(place: SamplePlace) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(place.heroImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 66.dp, height = 62.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 26.sp),
                )
                Text(text = place.area, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.outline)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = place.rating,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = 0.sp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stars(5),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = MaterialTheme.extendedColors.rating,
                )
                Text(
                    text = stringResource(R.string.reviews_google_count, place.reviewCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(review: SampleReview) {
    val colors = MaterialTheme.colorScheme
    var helpful by rememberSaveable(review.author) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = review.initials,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = colors.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = review.author, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                Text(text = stars(review.stars), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.extendedColors.rating)
            }
            // Review options (report, share) are not designed yet.
            Icon(
                painter = painterResource(R.drawable.ic_ellipsis),
                contentDescription = stringResource(R.string.cd_more_options),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(text = review.text, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.sp))
        Text(text = review.tags, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        HorizontalDivider(thickness = 1.dp, color = colors.outline)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Checkbox) { helpful = !helpful },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                val tint = if (helpful) colors.primary else colors.onSurfaceVariant
                Icon(
                    painter = painterResource(R.drawable.ic_thumbs_up),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(14.dp),
                )
                Text(text = stringResource(R.string.reviews_helpful), style = MaterialTheme.typography.bodySmall, color = tint)
            }
            Text(text = stringResource(R.string.reviews_report), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

/** Five star characters, [filled] of them solid. */
private fun stars(filled: Int) = "★".repeat(filled) + "☆".repeat(5 - filled)

@ThemePreviews
@Composable
private fun PlaceReviewsScreenPreview() {
    TripFairTheme {
        PlaceReviewsScreen(place = SamplePlaces.goldenBridge, onBack = {}, onWriteReview = {})
    }
}
