package com.tripfair.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.BrandMark
import com.tripfair.app.ui.components.FillHeightScrollColumn
import com.tripfair.app.ui.components.LabeledTextField
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TextWithLink
import com.tripfair.app.ui.components.TripFairCheckbox
import com.tripfair.app.ui.components.appendLink
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/** Connects [RegisterScreen] to its [RegisterViewModel]; [onCodeSent] opens the code screen. */
@Composable
fun RegisterRoute(
    onCodeSent: () -> Unit,
    onLogin: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state = viewModel.state
    LaunchedEffect(state.codeSent) {
        if (state.codeSent) {
            viewModel.onCodeSentHandled()
            onCodeSent()
        }
    }
    RegisterScreen(
        state = state,
        onFullNameChange = viewModel::onFullNameChange,
        onContactChange = viewModel::onContactChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onAcceptedTermsChange = viewModel::onAcceptedTermsChange,
        onCreateAccount = viewModel::submit,
        onLogin = onLogin,
        onTermsClick = onTermsClick,
        onPrivacyClick = onPrivacyClick,
    )
}

@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onFullNameChange: (String) -> Unit,
    onContactChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onAcceptedTermsChange: (Boolean) -> Unit,
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
) {
    Scaffold { innerPadding ->
        FillHeightScrollColumn(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(modifier = Modifier.weight(1f))
                    StepBadge(text = stringResource(R.string.register_step_badge))
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.register_title),
                        style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = (-0.6).sp),
                    )
                    Text(
                        text = stringResource(R.string.register_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    LabeledTextField(
                        value = state.fullName,
                        onValueChange = onFullNameChange,
                        label = stringResource(R.string.register_full_name_label),
                        leadingIcon = R.drawable.ic_user,
                        placeholder = stringResource(R.string.placeholder_name),
                        errorText = state.fullNameError?.let { stringResource(it) },
                    )
                    LabeledTextField(
                        value = state.contact,
                        onValueChange = onContactChange,
                        label = stringResource(R.string.field_contact_label),
                        leadingIcon = R.drawable.ic_mail,
                        placeholder = stringResource(R.string.placeholder_contact),
                        helperText = stringResource(R.string.register_contact_helper),
                        errorText = state.contactError?.let { stringResource(it) },
                        keyboardType = KeyboardType.Email,
                    )
                    LabeledTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = stringResource(R.string.field_password_label),
                        leadingIcon = R.drawable.ic_lock_keyhole,
                        placeholder = stringResource(R.string.placeholder_password),
                        helperText = stringResource(R.string.password_rule_helper),
                        errorText = state.passwordError?.let { stringResource(it) },
                        isPassword = true,
                    )
                    LabeledTextField(
                        value = state.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = stringResource(R.string.register_confirm_password_label),
                        leadingIcon = R.drawable.ic_shield_check,
                        placeholder = stringResource(R.string.placeholder_password),
                        errorText = state.confirmPasswordError?.let { stringResource(it) },
                        isPassword = true,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.toggleable(
                            value = state.acceptedTerms,
                            role = Role.Checkbox,
                            onValueChange = onAcceptedTermsChange,
                        ),
                    ) {
                        TripFairCheckbox(checked = state.acceptedTerms)
                        TermsText(
                            onTermsClick = onTermsClick,
                            onPrivacyClick = onPrivacyClick,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                    state.termsError?.let { error ->
                        Text(
                            text = stringResource(error),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                PrimaryButton(
                    text = stringResource(R.string.register_submit),
                    onClick = onCreateAccount,
                    trailingIcon = R.drawable.ic_arrow_right,
                    enabled = !state.loading,
                )
                TextWithLink(
                    prefix = stringResource(R.string.register_have_account),
                    link = stringResource(R.string.action_login),
                    onLinkClick = onLogin,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StepBadge(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(horizontal = 11.dp, vertical = 7.dp),
    )
}

@Composable
private fun TermsText(onTermsClick: () -> Unit, onPrivacyClick: () -> Unit, modifier: Modifier = Modifier) {
    val prefix = stringResource(R.string.register_terms_prefix)
    val terms = stringResource(R.string.terms_of_use)
    val and = stringResource(R.string.register_terms_and)
    val privacy = stringResource(R.string.privacy_policy)
    val suffix = stringResource(R.string.register_terms_suffix)
    val linkColor = MaterialTheme.colorScheme.primary
    val text = buildAnnotatedString {
        append("$prefix ")
        appendLink(terms, linkColor, onTermsClick, bold = true)
        append(" $and ")
        appendLink(privacy, linkColor, onPrivacyClick, bold = true)
        append(" $suffix")
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@ThemePreviews
@Composable
private fun RegisterScreenPreview() {
    TripFairTheme {
        RegisterScreen(
            state = RegisterUiState(acceptedTerms = true),
            onFullNameChange = {},
            onContactChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onAcceptedTermsChange = {},
            onCreateAccount = {},
            onLogin = {},
            onTermsClick = {},
            onPrivacyClick = {},
        )
    }
}
