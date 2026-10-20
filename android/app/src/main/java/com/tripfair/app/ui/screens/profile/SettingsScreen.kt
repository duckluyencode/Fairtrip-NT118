package com.tripfair.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.Chevron
import com.tripfair.app.ui.components.ListCard
import com.tripfair.app.ui.components.ListDivider
import com.tripfair.app.ui.components.ListRow
import com.tripfair.app.ui.components.TripFairSwitch
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleData
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/** Rows inside the settings cards sit in a padded card, unlike the "Tôi" list. */
private val SettingsRowPadding = PaddingValues(vertical = 6.dp)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onPrivacy: () -> Unit,
    onSupport: () -> Unit,
    onTerms: () -> Unit,
    onLogout: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var tripNotifications by rememberSaveable { mutableStateOf(true) }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    val sectionStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 26.sp)

    Scaffold(topBar = { TripFairTopBar(title = stringResource(R.string.settings_title), onBack = onBack) }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(colors.surfaceContainerLow)
                    .clickable(role = Role.Button, onClick = onEditProfile)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(colors.tertiaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_user_filled),
                        contentDescription = null,
                        tint = colors.tertiary,
                        modifier = Modifier.size(23.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = SampleData.USER_NAME,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
                    )
                    Text(text = SampleData.USER_EMAIL, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Chevron()
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.settings_preferences), style = sectionStyle)
                ListCard(contentPadding = PaddingValues(14.dp)) {
                    ListRow(
                        icon = R.drawable.ic_bell,
                        title = stringResource(R.string.settings_trip_notifications),
                        subtitle = stringResource(R.string.settings_trip_notifications_detail),
                        contentPadding = SettingsRowPadding,
                        trailing = { TripFairSwitch(checked = tripNotifications, onCheckedChange = { tripNotifications = it }) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.settings_account_security), style = sectionStyle)
                ListCard(contentPadding = PaddingValues(14.dp)) {
                    ListRow(
                        icon = R.drawable.ic_user_round,
                        title = stringResource(R.string.edit_profile_title),
                        onClick = onEditProfile,
                        contentPadding = SettingsRowPadding,
                    )
                    ListDivider()
                    ListRow(
                        icon = R.drawable.ic_shield_check,
                        title = stringResource(R.string.privacy_policy_row),
                        onClick = onPrivacy,
                        contentPadding = SettingsRowPadding,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.settings_info_support), style = sectionStyle)
                ListCard(contentPadding = PaddingValues(14.dp)) {
                    ListRow(
                        icon = R.drawable.ic_circle_help,
                        title = stringResource(R.string.settings_support_center),
                        onClick = onSupport,
                        contentPadding = SettingsRowPadding,
                    )
                    ListDivider()
                    ListRow(
                        icon = R.drawable.ic_file_text,
                        title = stringResource(R.string.terms_of_use),
                        onClick = onTerms,
                        contentPadding = SettingsRowPadding,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button) { confirmLogout = true }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_log_out),
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = stringResource(R.string.action_logout),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = colors.error,
                )
            }
        }
    }

    if (confirmLogout) {
        LogoutDialog(onConfirm = onLogout, onDismiss = { confirmLogout = false })
    }
}

@ThemePreviews
@Composable
private fun SettingsScreenPreview() {
    TripFairTheme {
        SettingsScreen(onBack = {}, onEditProfile = {}, onPrivacy = {}, onSupport = {}, onTerms = {}, onLogout = {})
    }
}
