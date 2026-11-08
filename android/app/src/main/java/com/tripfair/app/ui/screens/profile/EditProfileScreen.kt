package com.tripfair.app.ui.screens.profile

import androidx.annotation.DrawableRes
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.LabeledTextField
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleData
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun EditProfileRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state = viewModel.state
    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }
    EditProfileScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onSave = viewModel::save,
        onBack = onBack,
    )
}

@Composable
fun EditProfileScreen(
    state: EditProfileUiState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.edit_profile_title), onBack = onBack) },
        bottomBar = {
            Box(modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.edit_profile_save),
                    onClick = onSave,
                    height = 52.dp,
                    enabled = !state.loading,
                )
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
                    value = state.name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.register_full_name_label),
                    leadingIcon = R.drawable.ic_user_round,
                    errorText = state.nameError?.let { stringResource(it) },
                )
                ReadOnlyField(
                    label = stringResource(R.string.edit_profile_email),
                    value = state.email,
                    icon = R.drawable.ic_mail,
                )
                ReadOnlyField(
                    label = stringResource(R.string.edit_profile_phone),
                    value = state.phone,
                    icon = R.drawable.ic_phone,
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

/** A value that cannot be edited here, laid out like a text field. */
@Composable
private fun ReadOnlyField(label: String, value: String?, @DrawableRes icon: Int) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                .padding(horizontal = 14.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = value ?: stringResource(R.string.edit_profile_none),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun EditProfileScreenPreview() {
    TripFairTheme {
        EditProfileScreen(
            state = EditProfileUiState(name = SampleData.USER_NAME, email = SampleData.USER_EMAIL, phone = null),
            onNameChange = {},
            onSave = {},
            onBack = {},
        )
    }
}
