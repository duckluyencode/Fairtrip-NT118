package com.tripfair.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.tripfair.app.R

/** TripFair logo and name ("Nhận diện TripFair" in Figma). [large] adds the tagline, as on Login. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, large: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (large) 44.dp else 38.dp)
                .background(colors.primary, RoundedCornerShape(if (large) 15.dp else 13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_route),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(if (large) 24.dp else 21.dp),
            )
        }
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall.let {
                    if (large) it else it.copy(fontSize = 20.sp, lineHeight = 24.sp)
                },
            )
            if (large) {
                Text(
                    text = stringResource(R.string.brand_tagline),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.primary,
                )
            }
        }
    }
}
