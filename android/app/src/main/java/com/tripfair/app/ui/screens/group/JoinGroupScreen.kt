package com.tripfair.app.ui.screens.group

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleGroup
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

private const val GROUP_CODE_LENGTH = 6

@Composable
fun JoinGroupScreen(onBack: () -> Unit, onJoin: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var code by rememberSaveable { mutableStateOf(SampleGroup.CODE) }
    // With sample data, any full-length code "finds" the Đà Nẵng group.
    val found = code.length == GROUP_CODE_LENGTH

    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.join_group_title), onBack = onBack) },
        bottomBar = {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = stringResource(R.string.join_group_check), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                PrimaryButton(text = stringResource(R.string.group_join), onClick = onJoin, height = 52.dp)
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
                .padding(start = 20.dp, end = 20.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            GroupHeading(
                title = stringResource(R.string.join_group_heading),
                text = stringResource(R.string.join_group_intro),
            )
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                CapsLabel(text = stringResource(R.string.join_group_code_label))
                BasicTextField(
                    value = code,
                    onValueChange = { input -> code = input.filter { it.isLetterOrDigit() }.uppercase().take(GROUP_CODE_LENGTH) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    decorationBox = { inner ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                                .border(1.dp, colors.primary, MaterialTheme.shapes.small)
                                .padding(horizontal = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(modifier = Modifier.weight(1f)) { inner() }
                            // Clipboard access comes with the real join flow; for now it pastes the sample code.
                            Text(
                                text = stringResource(R.string.join_group_paste),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.primary,
                                modifier = Modifier
                                    .clickable(role = Role.Button) { code = SampleGroup.CODE }
                                    .padding(4.dp),
                            )
                        }
                    },
                )
                Text(text = stringResource(R.string.join_group_code_helper), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            if (found) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.join_group_found),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.primary,
                    )
                    GroupInfoCard()
                }
            }
            GroupHint(text = stringResource(R.string.join_group_hint))
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(text = stringResource(R.string.join_group_no_code), style = MaterialTheme.typography.titleSmall)
                Text(text = stringResource(R.string.join_group_no_code_text), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@ThemePreviews
@Composable
private fun JoinGroupScreenPreview() {
    TripFairTheme {
        JoinGroupScreen(onBack = {}, onJoin = {})
    }
}
