package com.tripfair.app.ui.screens.group

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tripfair.app.R
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun CreateOrJoinGroupScreen(onBack: () -> Unit, onCreate: () -> Unit, onJoin: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Scaffold(topBar = { TripFairTopBar(title = stringResource(R.string.group_choice_title), onBack = onBack) }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            GroupHeading(
                title = stringResource(R.string.group_choice_heading),
                text = stringResource(R.string.group_choice_intro),
            )
            ChoiceCard(
                icon = R.drawable.ic_users,
                title = stringResource(R.string.group_create),
                text = stringResource(R.string.group_create_detail),
            ) {
                PrimaryButton(text = stringResource(R.string.group_create), onClick = onCreate, height = 52.dp)
            }
            ChoiceCard(
                icon = R.drawable.ic_key_round,
                title = stringResource(R.string.group_join),
                text = stringResource(R.string.group_join_detail),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.group_join),
                    onClick = onJoin,
                    height = 52.dp,
                    containerColor = colors.primaryContainer,
                    contentColor = colors.primary,
                )
            }
            GroupHint(text = stringResource(R.string.group_choice_hint))
        }
    }
}

@Composable
private fun ChoiceCard(@DrawableRes icon: Int, title: String, text: String, action: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon = icon, size = 42.dp)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        action()
    }
}

@ThemePreviews
@Composable
private fun CreateOrJoinGroupPreview() {
    TripFairTheme {
        CreateOrJoinGroupScreen(onBack = {}, onCreate = {}, onJoin = {})
    }
}
