package com.tripfair.app.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.FillHeightScrollColumn
import com.tripfair.app.ui.components.InfoNote
import com.tripfair.app.ui.components.LabeledTextField
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TextWithLink
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/** Connects [ForgotPasswordScreen] to its [ForgotPasswordViewModel]; [onCodeSent] opens the code screen. */
@Composable
fun ForgotPasswordRoute(
    onBack: () -> Unit,
    onCodeSent: () -> Unit,
    onBackToLogin: () -> Unit,
    onSupport: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state = viewModel.state
    LaunchedEffect(state.codeSent) {
        if (state.codeSent) {
            viewModel.onCodeSentHandled()
            onCodeSent()
        }
    }
    ForgotPasswordScreen(
        state = state,
        onContactChange = viewModel::onContactChange,
        onBack = onBack,
        onSendCode = viewModel::sendCode,
        onBackToLogin = onBackToLogin,
        onSupport = onSupport,
    )
}

@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    onContactChange: (String) -> Unit,
    onBack: () -> Unit,
    onSendCode: () -> Unit,
    onBackToLogin: () -> Unit,
    onSupport: () -> Unit,
) {
    Scaffold(topBar = { AuthTopBar(title = stringResource(R.string.forgot_top_title), onBack = onBack) }) { innerPadding ->
        FillHeightScrollColumn(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                AuthIllustration(icon = R.drawable.ic_mail_check)
                AuthHeading(
                    title = stringResource(R.string.forgot_title),
                    subtitle = stringResource(R.string.forgot_subtitle),
                )
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LabeledTextField(
                        value = state.contact,
                        onValueChange = onContactChange,
                        label = stringResource(R.string.forgot_contact_label),
                        leadingIcon = R.drawable.ic_mail,
                        helperText = stringResource(R.string.forgot_contact_helper),
                        errorText = state.contactError?.let { stringResource(it) },
                        keyboardType = KeyboardType.Email,
                    )
                    InfoNote(icon = R.drawable.ic_clock, text = stringResource(R.string.forgot_code_validity_note))
                }
            }
            Column(
                modifier = Modifier.padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.forgot_submit),
                    onClick = onSendCode,
                    trailingIcon = R.drawable.ic_send,
                    enabled = !state.loading,
                )
                TextWithLink(
                    prefix = stringResource(R.string.forgot_remembered),
                    link = stringResource(R.string.back_to_login),
                    onLinkClick = onBackToLogin,
                    modifier = Modifier.fillMaxWidth(),
                )
                SupportCard(onClick = onSupport)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ForgotPasswordScreenPreview() {
    TripFairTheme {
        ForgotPasswordScreen(
            state = ForgotPasswordUiState(),
            onContactChange = {},
            onBack = {},
            onSendCode = {},
            onBackToLogin = {},
            onSupport = {},
        )
    }
}
