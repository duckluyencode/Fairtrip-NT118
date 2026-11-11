package com.tripfair.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.LabeledTextField
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun DeleteAccountRoute(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: DeleteAccountViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state = viewModel.state
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }
    DeleteAccountScreen(
        state = state,
        onPasswordChange = viewModel::onPasswordChange,
        onDelete = viewModel::delete,
        onBack = onBack,
    )
}

/** Explains what is deleted, asks for the password, then deletes for good (DELETE /me). */
@Composable
fun DeleteAccountScreen(
    state: DeleteAccountUiState,
    onPasswordChange: (String) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.delete_account_title), onBack = onBack) },
        bottomBar = {
            Box(modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.delete_account_confirm),
                    onClick = onDelete,
                    height = 52.dp,
                    containerColor = colors.error,
                    contentColor = colors.onError,
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.errorContainer, MaterialTheme.shapes.small)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_alert_triangle),
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.delete_account_intro),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = colors.onErrorContainer,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    R.string.delete_account_point_data,
                    R.string.delete_account_point_trips,
                    R.string.delete_account_point_groups,
                ).forEach { point ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Text(text = "•", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        Text(
                            text = stringResource(point),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
            LabeledTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.delete_account_password_label),
                leadingIcon = R.drawable.ic_lock_keyhole,
                errorText = state.passwordError?.let { stringResource(it) },
                isPassword = true,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun DeleteAccountScreenPreview() {
    TripFairTheme {
        DeleteAccountScreen(state = DeleteAccountUiState(), onPasswordChange = {}, onDelete = {}, onBack = {})
    }
}
