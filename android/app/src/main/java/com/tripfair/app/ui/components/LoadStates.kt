package com.tripfair.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tripfair.app.R

// States every screen that loads from the backend shows: loading, failed, nothing found.

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_circle_alert),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.action_retry), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
    )
}

/**
 * Stands in for a photo: places from Google come without one (photos are billed per
 * request and need an image loader), so the category's icon is shown instead.
 */
@Composable
fun PlacePlaceholder(category: String, modifier: Modifier = Modifier, iconSize: Int = 24) {
    val colors = MaterialTheme.colorScheme
    val (background, tint, icon) = when (category) {
        "FOOD" -> Triple(colors.tertiaryContainer, colors.tertiary, R.drawable.ic_utensils)
        "SCENIC" -> Triple(colors.primaryContainer, colors.primary, R.drawable.ic_mountain)
        else -> Triple(colors.secondaryContainer, colors.secondary, R.drawable.ic_compass)
    }
    Box(modifier = modifier.background(background), contentAlignment = Alignment.Center) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize.dp))
    }
}
