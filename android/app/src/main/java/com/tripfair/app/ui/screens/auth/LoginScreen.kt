package com.tripfair.app.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tripfair.app.R
import com.tripfair.app.ui.components.BrandMark
import com.tripfair.app.ui.components.FillHeightScrollColumn
import com.tripfair.app.ui.components.LabeledTextField
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TextWithLink
import com.tripfair.app.ui.components.TripFairCheckbox
import com.tripfair.app.ui.sample.SampleData
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun LoginScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    var contact by rememberSaveable { mutableStateOf(SampleData.USER_EMAIL) }
    var password by rememberSaveable { mutableStateOf(SampleData.PASSWORD) }
    var rememberMe by rememberSaveable { mutableStateOf(true) }

    Scaffold { innerPadding ->
        FillHeightScrollColumn(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            BrandMark(large = true)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.login_title), style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = stringResource(R.string.field_contact_label),
                    leadingIcon = R.drawable.ic_mail,
                    keyboardType = KeyboardType.Email,
                )
                LabeledTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.field_password_label),
                    leadingIcon = R.drawable.ic_lock_keyhole,
                    isPassword = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .toggleable(value = rememberMe, role = Role.Checkbox, onValueChange = { rememberMe = it }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TripFairCheckbox(checked = rememberMe)
                        Text(
                            text = stringResource(R.string.login_remember_me),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 9.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.login_forgot_password),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(role = Role.Button, onClick = onForgotPassword),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.action_login),
                    onClick = onLogin,
                    trailingIcon = R.drawable.ic_arrow_right,
                )
                TextWithLink(
                    prefix = stringResource(R.string.login_no_account),
                    link = stringResource(R.string.login_register_now),
                    onLinkClick = onRegister,
                )
                Text(
                    text = stringResource(R.string.login_security_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun LoginScreenPreview() {
    TripFairTheme {
        LoginScreen(onLogin = {}, onRegister = {}, onForgotPassword = {})
    }
}
