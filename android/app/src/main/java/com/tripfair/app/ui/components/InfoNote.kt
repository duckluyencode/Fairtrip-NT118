package com.tripfair.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Light teal note with an icon tile on the left, e.g. code validity or verified account. */
@Composable
fun InfoNote(
    @DrawableRes icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.primaryContainer, MaterialTheme.shapes.small)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(colors.surfaceContainerLow, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(if (title != null) 18.dp else 16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (title != null) {
                Text(text = title, style = type.bodySmall.copy(fontWeight = FontWeight.Bold), color = colors.primary)
            }
            Text(
                text = text,
                style = if (title != null) type.bodyMedium else type.bodySmall,
                color = colors.onPrimaryContainer,
            )
        }
    }
}
