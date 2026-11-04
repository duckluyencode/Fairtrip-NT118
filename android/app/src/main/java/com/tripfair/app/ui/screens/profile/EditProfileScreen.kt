package com.tripfair.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tripfair.app.R
import com.tripfair.app.ui.components.LabeledTextField
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleData
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun EditProfileScreen(onBack: () -> Unit, onSave: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var name by rememberSaveable { mutableStateOf(SampleData.USER_NAME) }
    var email by rememberSaveable { mutableStateOf(SampleData.USER_EMAIL) }
    var phone by rememberSaveable { mutableStateOf(SampleData.USER_PHONE) }

    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.edit_profile_title), onBack = onBack) },
        bottomBar = {
            Box(modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                PrimaryButton(text = stringResource(R.string.edit_profile_save), onClick = onSave, height = 52.dp)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Picking a new photo needs the gallery or camera, which comes later.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.size(88.dp)) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(colors.tertiaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_user_filled),
                            contentDescription = null,
                            tint = colors.tertiary,
                            modifier = Modifier.size(46.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = 60.dp, y = 60.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                            .border(3.dp, colors.background, CircleShape)
                            .clickable(role = Role.Button) {},
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_camera),
                            contentDescription = stringResource(R.string.edit_profile_change_photo),
                            tint = colors.onPrimary,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.edit_profile_change_photo),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = colors.primary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    text = stringResource(R.string.edit_profile_section),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                LabeledTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.register_full_name_label),
                    leadingIcon = R.drawable.ic_user_round,
                )
                LabeledTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = stringResource(R.string.edit_profile_email),
                    leadingIcon = R.drawable.ic_mail,
                    keyboardType = KeyboardType.Email,
                )
                LabeledTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = stringResource(R.string.edit_profile_phone),
                    leadingIcon = R.drawable.ic_phone,
                    keyboardType = KeyboardType.Phone,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_circle_help),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.edit_profile_verify_title),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    )
                    Text(
                        text = stringResource(R.string.edit_profile_verify_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun EditProfileScreenPreview() {
    TripFairTheme {
        EditProfileScreen(onBack = {}, onSave = {})
    }
}
