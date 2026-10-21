package com.tripfair.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.FillHeightScrollColumn
import com.tripfair.app.ui.components.InfoNote
import com.tripfair.app.ui.components.LabeledTextField
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.sample.SampleData
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/** Connects [NewPasswordScreen] to its [NewPasswordViewModel]; [onSaved] runs once the password is changed. */
@Composable
fun NewPasswordRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onSupport: () -> Unit,
    viewModel: NewPasswordViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state = viewModel.state
    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }
    NewPasswordScreen(
        state = state,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onBack = onBack,
        onSave = viewModel::save,
        onSupport = onSupport,
    )
}

@Composable
fun NewPasswordScreen(
    state: NewPasswordUiState,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSupport: () -> Unit,
) {
    Scaffold(
        topBar = { AuthTopBar(title = stringResource(R.string.new_password_top_title), onBack = onBack) },
    ) { innerPadding ->
        FillHeightScrollColumn(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                    AuthIllustration(icon = R.drawable.ic_lock_keyhole)
                    AuthHeading(
                        title = stringResource(R.string.new_password_title),
                        subtitle = stringResource(R.string.new_password_subtitle),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    InfoNote(
                        icon = R.drawable.ic_shield_check,
                        title = stringResource(R.string.new_password_verified),
                        text = state.contact,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LabeledTextField(
                            value = state.password,
                            onValueChange = onPasswordChange,
                            label = stringResource(R.string.new_password_label),
                            leadingIcon = R.drawable.ic_lock_keyhole,
                            helperText = stringResource(R.string.password_rule_helper),
                            errorText = state.passwordError?.let { stringResource(it) },
                            isPassword = true,
                        )
                        LabeledTextField(
                            value = state.confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            label = stringResource(R.string.new_password_confirm_label),
                            leadingIcon = R.drawable.ic_shield_check,
                            errorText = state.confirmPasswordError?.let { stringResource(it) },
                            isPassword = true,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton(
                        text = stringResource(R.string.new_password_submit),
                        onClick = onSave,
                        trailingIcon = R.drawable.ic_arrow_right,
                        enabled = !state.loading,
                    )
                    Text(
                        text = stringResource(R.string.new_password_after_save),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Column(modifier = Modifier.padding(top = 24.dp)) {
                SupportCard(onClick = onSupport)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun NewPasswordScreenPreview() {
    TripFairTheme {
        NewPasswordScreen(
            state = NewPasswordUiState(contact = SampleData.USER_EMAIL),
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onBack = {},
            onSave = {},
            onSupport = {},
        )
    }
}
