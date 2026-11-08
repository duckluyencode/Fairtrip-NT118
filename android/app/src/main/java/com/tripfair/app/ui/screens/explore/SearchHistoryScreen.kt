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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.places.HistoryEntryDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.EmptyState
import com.tripfair.app.ui.components.ErrorState
import com.tripfair.app.ui.components.LoadingState
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun SearchHistoryRoute(
    onBack: () -> Unit,
    onSearch: (query: String) -> Unit,
    viewModel: SearchHistoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    SearchHistoryScreen(
        state = viewModel.state,
        onBack = onBack,
        onSearch = onSearch,
        onRemove = viewModel::delete,
        onClear = viewModel::clear,
        onRetry = viewModel::load,
    )
}

@Composable
fun SearchHistoryScreen(
    state: SearchHistoryUiState,
    onBack: () -> Unit,
    onSearch: (query: String) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    // Newest first, as the backend sends them; one group per local day.
    val groups = state.entries.groupBy { Instant.parse(it.searchedAt).atZone(zone).toLocalDate() }
    val count = state.entries.size

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
            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(message = stringResource(state.error), onRetry = onRetry)
                count == 0 -> EmptyState(text = stringResource(R.string.history_empty))
                else -> {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pluralStringResource(R.plurals.history_count, count, count),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.history_clear),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.tertiary,
                            modifier = Modifier.clickable(role = Role.Button, onClick = onClear),
                        )
                    }
                    groups.forEach { (day, entries) ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = when (day) {
                                    today -> stringResource(R.string.history_today)
                                    today.minusDays(1) -> stringResource(R.string.history_yesterday)
                                    else -> day.format(DayFormat)
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 22.sp),
                            )
                            Column(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(colors.surfaceContainerLow),
                            ) {
                                entries.forEach { entry ->
                                    HistoryRow(
                                        entry = entry,
                                        time = Instant.parse(entry.searchedAt).atZone(zone).format(TimeFormat),
                                        onClick = { onSearch(entry.query) },
                                        onRemove = { onRemove(entry.id) },
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
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntryDto, time: String, onClick: () -> Unit, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val filter = entry.category?.let { stringResource(categoryLabel(it)) } ?: stringResource(R.string.filter_all)
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
                    text = entry.query,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
                )
                Text(text = "$filter · $time", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
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
        SearchHistoryScreen(
            state = SearchHistoryUiState(
                loading = false,
                entries = listOf(
                    HistoryEntryDto(1, "Cầu Vàng", null, Instant.now().toString()),
                    HistoryEntryDto(2, "Mì Quảng", "FOOD", Instant.now().minusSeconds(86_400).toString()),
                ),
            ),
            onBack = {},
            onSearch = {},
            onRemove = {},
            onClear = {},
            onRetry = {},
        )
    }
}
