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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tripfair.app.R
import com.tripfair.app.ui.components.FillHeightScrollColumn
import com.tripfair.app.ui.components.InfoNote
import com.tripfair.app.ui.components.LabeledTextField
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.sample.SampleData
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun NewPasswordScreen(
    email: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSupport: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf(SampleData.PASSWORD) }
    var confirmPassword by rememberSaveable { mutableStateOf(SampleData.PASSWORD) }

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
                        text = email,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LabeledTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = stringResource(R.string.new_password_label),
                            leadingIcon = R.drawable.ic_lock_keyhole,
                            helperText = stringResource(R.string.password_rule_helper),
                            isPassword = true,
                        )
                        LabeledTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = stringResource(R.string.new_password_confirm_label),
                            leadingIcon = R.drawable.ic_shield_check,
                            isPassword = true,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton(
                        text = stringResource(R.string.new_password_submit),
                        onClick = onSave,
                        trailingIcon = R.drawable.ic_arrow_right,
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
        NewPasswordScreen(email = SampleData.USER_EMAIL, onBack = {}, onSave = {}, onSupport = {})
    }
}
