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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.FillHeightScrollColumn
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.sample.SampleData
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

private const val OTP_LENGTH = 6

@Composable
fun OtpVerificationScreen(
    destination: String,
    onBack: () -> Unit,
    onVerified: () -> Unit,
    onChangeDestination: () -> Unit,
    onBackToLogin: () -> Unit,
    onSupport: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    var code by rememberSaveable { mutableStateOf(SampleData.OTP_CODE) }

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
                        Text(text = destination, style = type.titleSmall)
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
                        OtpCodeField(code = code, onCodeChange = { code = it })
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
                        onClick = onVerified,
                        trailingIcon = R.drawable.ic_arrow_right,
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
                        // Resend stays locked until the countdown ends; only the locked state is designed.
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .background(colors.outline, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.otp_resend_countdown, SampleData.OTP_RESEND_IN),
                                style = type.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.onSurfaceVariant,
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

/** Six boxes that show the typed digits ("Mã sáu chữ số" in Figma), backed by one hidden text field. */
@Composable
private fun OtpCodeField(code: String, onCodeChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = code,
        onValueChange = { input -> onCodeChange(input.filter { it.isDigit() }.take(OTP_LENGTH)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(OTP_LENGTH) { index ->
                    val isNext = index == code.length
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                            .border(
                                width = 1.dp,
                                color = if (isNext) colors.primary else colors.outline,
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
            destination = SampleData.USER_EMAIL,
            onBack = {},
            onVerified = {},
            onChangeDestination = {},
            onBackToLogin = {},
            onSupport = {},
        )
    }
}
