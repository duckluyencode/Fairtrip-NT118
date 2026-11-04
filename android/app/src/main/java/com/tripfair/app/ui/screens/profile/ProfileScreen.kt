package com.tripfair.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.Chevron
import com.tripfair.app.ui.components.ConfirmDialog
import com.tripfair.app.ui.components.ListCard
import com.tripfair.app.ui.components.ListDivider
import com.tripfair.app.ui.components.ListRow
import com.tripfair.app.ui.components.MainTab
import com.tripfair.app.ui.components.MainTabBar
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleData
import com.tripfair.app.ui.sample.SampleTrip
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import com.tripfair.app.ui.theme.extendedColors

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onTrip: () -> Unit,
    onFavorites: () -> Unit,
    onEmergencyContacts: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onLogout: () -> Unit,
    onTabSelect: (MainTab) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(R.string.profile_title)) {
                Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
        },
        bottomBar = { MainTabBar(selected = MainTab.Profile, onSelect = onTabSelect) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfileCard(onEdit = onEditProfile)
            ListCard {
                ListRow(
                    icon = R.drawable.ic_contact_round,
                    title = stringResource(R.string.profile_account),
                    subtitle = stringResource(R.string.profile_account_detail, SampleData.USER_PHONE),
                    onClick = onEditProfile,
                    minHeight = 56.dp,
                )
                ListDivider()
                // Security settings are marked "cần bổ sung" in the design; no screen yet.
                ListRow(
                    icon = R.drawable.ic_shield_check,
                    title = stringResource(R.string.profile_security),
                    subtitle = stringResource(R.string.profile_security_detail),
                    minHeight = 56.dp,
                )
            }
            ListCard {
                TripHistoryRow(onClick = onTrip)
                ListDivider()
                ListRow(icon = R.drawable.ic_heart, title = stringResource(R.string.profile_favorites), onClick = onFavorites)
            }
            SosCard(onClick = onEmergencyContacts)
            ListCard {
                ListRow(icon = R.drawable.ic_settings, title = stringResource(R.string.profile_settings), onClick = onSettings)
                ListDivider()
                ListRow(
                    icon = R.drawable.ic_bell,
                    title = stringResource(R.string.profile_notifications),
                    onClick = onSettings,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = stringResource(R.string.profile_notifications_on),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                            )
                            Chevron()
                        }
                    },
                )
                ListDivider()
                ListRow(icon = R.drawable.ic_circle_help, title = stringResource(R.string.profile_help), onClick = onHelp)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(colors.surfaceContainerLow)
                    .border(BorderStroke(1.dp, colors.outline), MaterialTheme.shapes.small)
                    .clickable(role = Role.Button) { confirmLogout = true },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_log_out),
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(17.dp),
                )
                Text(text = stringResource(R.string.action_logout), style = MaterialTheme.typography.titleSmall, color = colors.error)
            }
        }
    }

    if (confirmLogout) {
        LogoutDialog(onConfirm = onLogout, onDismiss = { confirmLogout = false })
    }
}

@Composable
internal fun LogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.logout_confirm_title),
        text = stringResource(R.string.logout_confirm_text),
        confirmLabel = stringResource(R.string.action_logout),
        dismissLabel = stringResource(R.string.action_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmColor = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun ProfileCard(onEdit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(colors.surfaceContainerLow)
            .clickable(role = Role.Button, onClick = onEdit)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(colors.tertiaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_user_filled),
                contentDescription = null,
                tint = colors.tertiary,
                modifier = Modifier.size(30.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = SampleData.USER_NAME, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, letterSpacing = 0.sp))
            Text(text = SampleData.USER_EMAIL, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(
                text = stringResource(R.string.profile_edit_link),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = colors.primary,
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(colors.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pencil),
                contentDescription = stringResource(R.string.profile_edit_link),
                tint = colors.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun TripHistoryRow(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.sample_trip_da_nang),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 54.dp, height = 66.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(R.string.profile_trip_history), style = MaterialTheme.typography.titleSmall)
            Text(
                text = SampleTrip.NAME,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primary,
            )
            Text(
                text = pluralStringResource(R.plurals.profile_trip_members, SampleTrip.MEMBER_COUNT, SampleTrip.DATE_RANGE, SampleTrip.MEMBER_COUNT),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.profile_trip_status),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                color = colors.primary,
            )
        }
        Chevron()
    }
}

@Composable
private fun SosCard(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    ListRow(
        icon = R.drawable.ic_circle_alert,
        title = stringResource(R.string.profile_sos),
        subtitle = SampleData.SOS_CONTACTS_SUMMARY,
        onClick = onClick,
        iconTint = colors.error,
        iconBackground = colors.errorContainer,
        contentPadding = PaddingValues(12.dp),
        trailing = { Chevron(tint = colors.error) },
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(extended.errorSurface)
            .border(BorderStroke(1.dp, extended.errorBorder), MaterialTheme.shapes.small),
    )
}

@ThemePreviews
@Composable
private fun ProfileScreenPreview() {
    TripFairTheme {
        ProfileScreen(
            onEditProfile = {},
            onTrip = {},
            onFavorites = {},
            onEmergencyContacts = {},
            onSettings = {},
            onHelp = {},
            onLogout = {},
            onTabSelect = {},
        )
    }
}
