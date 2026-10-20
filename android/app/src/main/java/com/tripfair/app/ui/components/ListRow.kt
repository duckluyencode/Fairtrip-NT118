package com.tripfair.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tripfair.app.R

/** White rounded card that stacks [ListRow]s with dividers between them. */
@Composable
fun ListCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun ListDivider() {
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
}

/**
 * Row with a round icon tile, a title, an optional subtitle and trailing content
 * ("Mục tài khoản" / "List row" in Figma). [trailing] defaults to a chevron.
 */
@Composable
fun ListRow(
    @DrawableRes icon: Int,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    minHeight: Dp = 44.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    trailing: @Composable () -> Unit = { Chevron() },
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
fun Chevron(tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(16.dp),
    )
}

/** Small 38x23 switch from the settings design ("Switch" in Figma). */
@Composable
fun TripFairSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val thumbOffset by animateDpAsState(if (checked) 15.dp else 0.dp, label = "thumb")
    Box(
        modifier = modifier
            .size(width = 38.dp, height = 23.dp)
            .clip(CircleShape)
            .background(if (checked) colors.primary else colors.outline)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                .size(17.dp)
                .background(Color.White, CircleShape),
        )
    }
}
