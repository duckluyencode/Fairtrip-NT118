package com.tripfair.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ChipStyle {
    /** Selected chip is filled teal with white text (place filters). */
    Solid,

    /** Selected chip is light teal with teal text (review filters). */
    Soft,
}

/** Pill-shaped filter chip ("Bộ lọc" in Figma). */
@Composable
fun TripFairChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChipStyle = ChipStyle.Solid,
) {
    val colors = MaterialTheme.colorScheme
    val (background, border, content) = when {
        !selected -> Triple(colors.surfaceContainerLow, colors.outline, if (style == ChipStyle.Soft) colors.onSurfaceVariant else colors.onSurface)
        style == ChipStyle.Solid -> Triple(colors.primary, colors.primary, colors.onPrimary)
        else -> Triple(colors.primaryContainer, colors.primaryContainer, colors.primary)
    }
    val textStyle = if (style == ChipStyle.Solid) {
        MaterialTheme.typography.bodyMedium
    } else {
        MaterialTheme.typography.bodySmall.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
    Text(
        text = text,
        style = textStyle,
        color = content,
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .border(1.dp, border, CircleShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = if (style == ChipStyle.Solid) 8.dp else 7.dp),
    )
}
