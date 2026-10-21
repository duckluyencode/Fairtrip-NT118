package com.tripfair.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Full-width white button with a teal outline, e.g. "Chỉnh ngân sách của bạn".
 * [subtle] uses the thin grey outline of "Thêm liên hệ khẩn cấp".
 */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes trailingIcon: Int? = null,
    @DrawableRes leadingIcon: Int? = null,
    height: Dp = 52.dp,
    subtle: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = MaterialTheme.shapes.small,
        border = if (subtle) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        } else {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        },
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        if (leadingIcon != null) {
            Icon(painter = painterResource(leadingIcon), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
        if (trailingIcon != null) {
            Spacer(Modifier.width(9.dp))
            Icon(painter = painterResource(trailingIcon), contentDescription = null, modifier = Modifier.size(19.dp))
        }
    }
}

/** Full-width filled button ("Nút chính" in Figma). A disabled button is drawn at 40% opacity. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes trailingIcon: Int? = null,
    @DrawableRes leadingIcon: Int? = null,
    height: Dp = 54.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        if (leadingIcon != null) {
            Icon(painter = painterResource(leadingIcon), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
        if (trailingIcon != null) {
            Spacer(Modifier.width(9.dp))
            Icon(
                painter = painterResource(trailingIcon),
                contentDescription = null,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}
