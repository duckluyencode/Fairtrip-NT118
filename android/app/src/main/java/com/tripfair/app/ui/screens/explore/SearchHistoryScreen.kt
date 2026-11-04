package com.tripfair.app.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleSearch
import com.tripfair.app.ui.sample.SearchHistoryItem
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun SearchHistoryScreen(
    onBack: () -> Unit,
    onSearch: (query: String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // Removing items only changes this screen (Figma: "xóa lịch sử: thay đổi tại chỗ").
    var removedIds by rememberSaveable { mutableStateOf(setOf<Int>()) }
    val groups = SampleSearch.history
        .map { group -> group.copy(items = group.items.filter { it.id !in removedIds }) }
        .filter { it.items.isNotEmpty() }
    val count = groups.sumOf { it.items.size }

    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.history_title), onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pluralStringResource(R.plurals.history_count, count, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (count > 0) {
                    Text(
                        text = stringResource(R.string.history_clear),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.tertiary,
                        modifier = Modifier.clickable(role = Role.Button) {
                            removedIds = SampleSearch.history.flatMap { g -> g.items.map { it.id } }.toSet()
                        },
                    )
                }
            }
            groups.forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 22.sp),
                    )
                    Column(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(colors.surfaceContainerLow),
                    ) {
                        group.items.forEach { item ->
                            HistoryRow(
                                item = item,
                                onClick = { onSearch(item.query) },
                                onRemove = { removedIds = removedIds + item.id },
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.history_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colors.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primaryContainer, MaterialTheme.shapes.extraSmall)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun HistoryRow(item: SearchHistoryItem, onClick: () -> Unit, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(colors.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_history),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = item.query,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
                )
                Text(text = item.detail, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(R.drawable.ic_x),
                    contentDescription = stringResource(R.string.cd_clear),
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.outline)
    }
}

@ThemePreviews
@Composable
private fun SearchHistoryScreenPreview() {
    TripFairTheme {
        SearchHistoryScreen(onBack = {}, onSearch = {})
    }
}
