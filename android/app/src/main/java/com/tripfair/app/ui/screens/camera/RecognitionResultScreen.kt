package com.tripfair.app.ui.screens.camera

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TopBarTextAction
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.PhotoRecognition
import com.tripfair.app.ui.sample.PhotoSuggestion
import com.tripfair.app.ui.sample.SampleCamera
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import com.tripfair.app.ui.theme.extendedColors

/**
 * "Kết quả nhận diện": the photo with the landmark (or "Chưa nhận ra địa danh"), the labels
 * from Google Cloud Vision and nearby places that can be added to the trip.
 */
@Composable
fun RecognitionResultScreen(
    result: PhotoRecognition,
    onBack: () -> Unit,
    onRetake: () -> Unit,
    onPlace: (placeId: String) -> Unit,
    onViewTrip: () -> Unit,
    onSearchByName: () -> Unit,
) {
    // Places added to the trip from this screen, starting with those already in it.
    var added by rememberSaveable { mutableStateOf(result.suggestions.filter { it.inTrip }.map { it.name }.toSet()) }

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
            PhotoCard(result = result, onPlace = onPlace)
            LabelsCard(labels = result.labels)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(if (result.recognized) R.string.recognition_similar else R.string.recognition_by_labels),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.recognition_for_trip, SampleCamera.TRIP_NAME),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                result.suggestions.forEach { suggestion ->
                    SuggestionRow(
                        suggestion = suggestion,
                        added = suggestion.name in added,
                        onToggle = {
                            added = if (suggestion.name in added) added - suggestion.name else added + suggestion.name
                        },
                        onOpen = suggestion.placeId?.let { id -> { onPlace(id) } },
                    )
                }
            }
            if (result.recognized) {
                PrimaryButton(
                    text = stringResource(R.string.recognition_view_trip),
                    onClick = onViewTrip,
                    leadingIcon = R.drawable.ic_calendar_days,
                    height = 52.dp,
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.recognition_search_by_name),
                    onClick = onSearchByName,
                    leadingIcon = R.drawable.ic_search,
                    height = 52.dp,
                )
            }
        }
    }
}

/** The analyzed photo with the AI tag and the main result ("Ảnh đã nhận diện"). */
@Composable
private fun PhotoCard(result: PhotoRecognition, onPlace: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val camera = MaterialTheme.extendedColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(MaterialTheme.shapes.large),
    ) {
        Image(
            painter = painterResource(result.photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (result.recognized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(camera.cameraBackground.copy(alpha = 0.18f)),
            )
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
                    text = stringResource(if (result.recognized) R.string.recognition_found else R.string.recognition_not_found),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = camera.onCamera,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(colors.surfaceContainerLow.copy(alpha = 0.93f))
                    .then(if (result.placeId != null) Modifier.clickable(role = Role.Button) { onPlace(result.placeId) } else Modifier)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(text = result.title, style = MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp))
                    Text(text = result.subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                if (result.confidence != null) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = result.confidence, style = MaterialTheme.typography.titleMedium, color = colors.primary)
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

/** "Nhãn trong ảnh": the labels found by Google Cloud Vision; the first one is the strongest. */
@Composable
private fun LabelsCard(labels: List<String>) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.recognition_labels), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(text = stringResource(R.string.recognition_source), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            labels.forEachIndexed { index, label ->
                val top = index == 0
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (top) colors.onPrimary else colors.onSurface,
                    modifier = Modifier
                        .background(if (top) colors.primary else colors.surfaceContainerLow, CircleShape)
                        .border(1.dp, if (top) colors.primary else colors.outline, CircleShape)
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                )
            }
        }
    }
}

/** Suggested place with a round add button that turns into a check once added. */
@Composable
private fun SuggestionRow(
    suggestion: PhotoSuggestion,
    added: Boolean,
    onToggle: () -> Unit,
    onOpen: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.extraSmall)
                .then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(suggestion.image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 68.dp, height = 60.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = suggestion.name, style = MaterialTheme.typography.titleSmall)
                Text(text = suggestion.detail, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
        val label = stringResource(R.string.cd_add_to_trip, suggestion.name)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (added) colors.primaryContainer else colors.surfaceContainerLow)
                .border(1.dp, if (added) colors.primaryContainer else colors.outline, CircleShape)
                .toggleable(value = added, role = Role.Checkbox, onValueChange = { onToggle() })
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (added) R.drawable.ic_check else R.drawable.ic_plus),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun RecognitionResultPreview() {
    TripFairTheme {
        RecognitionResultScreen(
            result = SampleCamera.recognized,
            onBack = {},
            onRetake = {},
            onPlace = {},
            onViewTrip = {},
            onSearchByName = {},
        )
    }
}

@ThemePreviews
@Composable
private fun NotRecognizedPreview() {
    TripFairTheme {
        RecognitionResultScreen(
            result = SampleCamera.notRecognized,
            onBack = {},
            onRetake = {},
            onPlace = {},
            onViewTrip = {},
            onSearchByName = {},
        )
    }
}
