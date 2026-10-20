package com.tripfair.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tripfair.app.R

/**
 * 20dp rounded checkbox ("Ô chọn" in Figma). Only draws the box; the row that contains it
 * handles the click so the label is tappable too.
 */
@Composable
fun TripFairCheckbox(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .size(20.dp)
            .background(if (checked) colors.primary else colors.surfaceContainerLow, shape)
            .border(1.dp, if (checked) colors.primary else colors.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}
