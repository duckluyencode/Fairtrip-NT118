package com.tripfair.app.ui.screens.group

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.sample.SampleGroup
import com.tripfair.app.ui.theme.extendedColors

/**
 * Colored circle standing in for a member photo; the color follows the member's position.
 * [initial] shows the first letter of the name, as in the small avatars of the vote screens.
 */
@Composable
internal fun MemberAvatar(index: Int, size: Dp, initial: String? = null) {
    val palette = MaterialTheme.extendedColors.memberColors
    Box(
        modifier = Modifier
            .size(size)
            .background(palette[index % palette.size], CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (initial != null) {
            // Member colors stay the same in dark mode, so the letter stays white.
            Text(
                text = initial,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
    }
}

/** Small grey pill on the right of a member row ("Trưởng nhóm", "Đã mời", a budget). */
@Composable
internal fun MemberBadge(text: String, large: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = if (large) 11.sp else 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
        ),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(horizontal = if (large) 10.dp else 8.dp, vertical = if (large) 7.dp else 6.dp),
    )
}

/** Light teal note with the "person tick" icon ("Gợi ý" / "Bước tiếp theo" in Figma). */
@Composable
internal fun GroupHint(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_person_tick),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

/** Upper-case grey field label, e.g. "TÊN NHÓM". */
@Composable
internal fun CapsLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Rounded square icon tile in light teal. */
@Composable
internal fun IconTile(@DrawableRes icon: Int, size: Dp = 40.dp, iconSize: Dp = 20.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize))
    }
}

/** White card naming the group and its trip ("Thông tin nhóm" in Figma). */
@Composable
internal fun GroupInfoCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon = R.drawable.ic_users)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = SampleGroup.NAME, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp))
            Text(
                text = SampleGroup.SHORT_DETAIL,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Peach note that restates the fairness rule ("Nguyên tắc" in Figma). */
@Composable
internal fun FairnessNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.shapes.small)
            .padding(14.dp),
    )
}

/** 24sp heading with a muted paragraph, used at the top of the group forms. */
@Composable
internal fun GroupHeading(title: String, text: String, extra: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, lineHeight = 29.sp),
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (extra != null) {
            Text(
                text = extra,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
