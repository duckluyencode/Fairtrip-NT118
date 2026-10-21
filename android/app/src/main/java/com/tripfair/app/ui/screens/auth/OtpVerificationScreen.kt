package com.tripfair.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.auth.AuthValidation
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.FillHeightScrollColumn
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.sample.SampleData
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/** Connects [OtpVerificationScreen] to its [OtpViewModel]; [onVerified] runs once the code is accepted. */
@Composable
fun OtpRoute(
    onBack: () -> Unit,
    onVerified: () -> Unit,
    onChangeDestination: () -> Unit,
    onBackToLogin: () -> Unit,
    onSupport: () -> Unit,
    viewModel: OtpViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state = viewModel.state
    LaunchedEffect(state.verified) {
        if (state.verified) {
            viewModel.onVerifiedHandled()
            onVerified()
        }
    }
    OtpVerificationScreen(
        state = state,
        onCodeChange = viewModel::onCodeChange,
        onVerify = viewModel::verify,
        onResend = viewModel::resend,
        onBack = onBack,
        onChangeDestination = onChangeDestination,
        onBackToLogin = onBackToLogin,
        onSupport = onSupport,
    )
}

@Composable
fun OtpVerificationScreen(
    state: OtpUiState,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    onChangeDestination: () -> Unit,
    onBackToLogin: () -> Unit,
    onSupport: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    Scaffold(topBar = { AuthTopBar(title = stringResource(R.string.otp_top_title), onBack = onBack) }) { innerPadding ->
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
                    title = stringResource(R.string.otp_title),
                    subtitle = stringResource(R.string.otp_subtitle),
                ) {
                    Column {
                        Text(text = state.destination, style = type.titleSmall)
                        Text(
                            text = stringResource(R.string.otp_change_destination),
                            style = type.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primary,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable(role = Role.Button, onClick = onChangeDestination)
                                .padding(vertical = 7.dp),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = stringResource(R.string.otp_code_label), style = type.labelMedium)
                        val error = state.codeError?.let { stringResource(it) }
                        OtpCodeField(code = state.code, onCodeChange = onCodeChange, error = error)
                        if (error != null) {
                            Text(text = error, style = type.labelSmall, color = colors.error)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_clock),
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.otp_validity),
                            style = type.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    PrimaryButton(
                        text = stringResource(R.string.otp_submit),
                        onClick = onVerify,
                        trailingIcon = R.drawable.ic_arrow_right,
                        enabled = !state.loading,
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.otp_not_received),
                            style = type.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                        // Locked (grey, Figma) while counting down; when it ends it becomes a teal button.
                        val canResend = state.resendInSeconds == 0
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (canResend) colors.primaryContainer else colors.outline)
                                .clickable(enabled = canResend, role = Role.Button, onClick = onResend)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (canResend) {
                                    stringResource(R.string.otp_resend)
                                } else {
                                    stringResource(R.string.otp_resend_countdown, formatCountdown(state.resendInSeconds))
                                },
                                style = type.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (canResend) colors.primary else colors.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = stringResource(R.string.otp_spam_hint),
                            style = type.bodySmall,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                Text(
                    text = stringResource(R.string.back_to_login),
                    style = type.bodyLarge,
                    color = colors.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onBackToLogin),
                )
                SupportCard(onClick = onSupport)
            }
        }
    }
}

/** "01:05" style countdown for "Gửi lại mã sau …". */
private fun formatCountdown(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

/**
 * Six boxes that show the typed digits ("Mã sáu chữ số" in Figma), backed by one hidden text field.
 * With an [error] every box gets a red border.
 */
@Composable
private fun OtpCodeField(code: String, onCodeChange: (String) -> Unit, error: String?) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = code,
        onValueChange = onCodeChange,
        modifier = if (error != null) Modifier.semantics { error(error) } else Modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(AuthValidation.CODE_LENGTH) { index ->
                    val isNext = index == code.length
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                            .border(
                                width = 1.dp,
                                color = when {
                                    error != null -> colors.error
                                    isNext -> colors.primary
                                    else -> colors.outline
                                },
                                shape = MaterialTheme.shapes.small,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = code.getOrNull(index)?.toString() ?: "",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp,
                            ),
                        )
                    }
                }
            }
        },
    )
}

@ThemePreviews
@Composable
private fun OtpVerificationScreenPreview() {
    TripFairTheme {
        OtpVerificationScreen(
            state = OtpUiState(destination = SampleData.USER_EMAIL, resendInSeconds = 45),
            onCodeChange = {},
            onVerify = {},
            onResend = {},
            onBack = {},
            onChangeDestination = {},
            onBackToLogin = {},
            onSupport = {},
        )
    }
}
