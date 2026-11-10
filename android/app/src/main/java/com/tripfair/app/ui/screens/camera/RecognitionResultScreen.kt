package com.tripfair.app.ui.screens.camera

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.data.recognition.RecognitionDto
import com.tripfair.app.data.recognition.RecognizedPlaceDto
import com.tripfair.app.ui.components.Chevron
import com.tripfair.app.ui.components.OutlineButton
import com.tripfair.app.ui.components.PlacePlaceholder
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TopBarTextAction
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.screens.explore.categoryLabel
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import com.tripfair.app.ui.theme.extendedColors
import kotlin.math.roundToInt

/**
 * "Kết quả nhận diện": the photo with the landmark Google Cloud Vision saw in it (or
 * "Chưa nhận ra địa danh"), and the matching places, which open their detail screens.
 */
@Composable
fun RecognitionResultScreen(
    image: ImageBitmap?,
    result: RecognitionDto,
    onBack: () -> Unit,
    onRetake: () -> Unit,
    onPlace: (placeId: Int) -> Unit,
    onSearchByName: () -> Unit,
) {
    val best = result.items.firstOrNull()
    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(R.string.recognition_title), onBack = onBack) {
                TopBarTextAction(text = stringResource(R.string.recognition_retake), onClick = onRetake)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PhotoCard(image = image, best = best, onPlace = onPlace)
            if (result.items.isNotEmpty()) {
                Text(text = stringResource(R.string.recognition_matches), style = MaterialTheme.typography.titleMedium)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    result.items.forEach { item -> MatchRow(item = item, onPlace = onPlace) }
                }
            }
            result.attribution?.let {
                Text(
                    text = stringResource(R.string.detail_source, it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            best?.place?.let { place ->
                PrimaryButton(
                    text = stringResource(R.string.recognition_open_place, place.name),
                    onClick = { onPlace(place.id) },
                    trailingIcon = R.drawable.ic_arrow_right,
                    height = 52.dp,
                )
            }
            OutlineButton(text = stringResource(R.string.recognition_search_by_name), onClick = onSearchByName, leadingIcon = R.drawable.ic_search)
        }
    }
}

/** The analyzed photo with the AI tag and the main result ("Ảnh đã nhận diện"). */
@Composable
private fun PhotoCard(image: ImageBitmap?, best: RecognizedPlaceDto?, onPlace: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val camera = MaterialTheme.extendedColors
    val place = best?.place
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(MaterialTheme.shapes.large)
            .background(camera.cameraBackground),
    ) {
        if (image != null) {
            Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .background(camera.cameraBackground.copy(alpha = 0.72f), CircleShape)
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(painter = painterResource(R.drawable.ic_scan), contentDescription = null, tint = camera.onCamera, modifier = Modifier.size(15.dp))
                Text(
                    text = stringResource(if (best != null) R.string.recognition_found else R.string.recognition_not_found),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = camera.onCamera,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(colors.surfaceContainerLow.copy(alpha = 0.93f))
                    .then(if (place != null) Modifier.clickable(role = Role.Button) { onPlace(place.id) } else Modifier)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = place?.name ?: best?.landmark ?: stringResource(R.string.recognition_none_title),
                        style = MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp),
                    )
                    Text(
                        text = place?.area ?: stringResource(if (best != null) R.string.recognition_no_place else R.string.recognition_none_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (best != null) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = percent(best.confidence), style = MaterialTheme.typography.titleMedium, color = colors.primary)
                        Text(
                            text = stringResource(R.string.recognition_confidence),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun percent(confidence: Double): String = "${(confidence * 100).roundToInt()}%"

/** One landmark Vision saw: its place on TripFair when there is one, opened by tapping. */
@Composable
private fun MatchRow(item: RecognizedPlaceDto, onPlace: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val place = item.place
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .then(if (place != null) Modifier.clickable(role = Role.Button) { onPlace(place.id) } else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlacePlaceholder(
            category = place?.category.orEmpty(),
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.extraSmall),
            iconSize = 20,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = place?.name ?: item.landmark, style = MaterialTheme.typography.titleSmall)
            val detail = if (place != null) {
                listOfNotNull(place.area, stringResource(categoryLabel(place.category)), percent(item.confidence)).joinToString(" · ")
            } else {
                stringResource(R.string.recognition_no_place)
            }
            Text(text = detail, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        if (place != null) Chevron()
    }
}

@ThemePreviews
@Composable
private fun NotRecognizedPreview() {
    TripFairTheme {
        RecognitionResultScreen(image = null, result = RecognitionDto(), onBack = {}, onRetake = {}, onPlace = {}, onSearchByName = {})
    }
}

@ThemePreviews
@Composable
private fun RecognizedWithoutPlacePreview() {
    TripFairTheme {
        RecognitionResultScreen(
            image = null,
            result = RecognitionDto(items = listOf(RecognizedPlaceDto("Dragon Bridge", 0.96)), attribution = "Google Maps"),
            onBack = {},
            onRetake = {},
            onPlace = {},
            onSearchByName = {},
        )
    }
}
