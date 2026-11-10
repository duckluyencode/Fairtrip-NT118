package com.tripfair.app.ui.screens.explore

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.places.SuggestionDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.SectionHeader
import com.tripfair.app.ui.components.TopBarTextAction
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onSearch: (query: String) -> Unit,
    onPlace: (placeId: Int, sessionToken: String) -> Unit,
    onHistory: () -> Unit,
    viewModel: SearchViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onPick: ((SuggestionDto) -> Unit)? = null,
) {
    SearchScreen(
        state = viewModel.state,
        onQueryChange = viewModel::onQueryChange,
        onBack = onBack,
        // When picking a place for an itinerary, a submitted or recent query only fills the
        // box: the choice is made from the suggestions under it.
        onSearch = if (onPick != null) viewModel::onQueryChange else onSearch,
        onSuggestion = { if (onPick != null) onPick(it) else onPlace(it.id, viewModel.sessionToken) },
        onHistory = onHistory,
    )
}

@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSearch: (query: String) -> Unit,
    onSuggestion: (SuggestionDto) -> Unit,
    onHistory: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // Open the keyboard on the first visit only, not when coming back from results.
    var autoFocused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoFocused) {
            focusRequester.requestFocus()
            autoFocused = true
        }
    }

    // Close the keyboard before leaving, otherwise it stays open over the next screen.
    val leaveWith: (() -> Unit) -> Unit = { action ->
        focusManager.clearFocus()
        action()
    }
    val submit = { if (state.query.isNotBlank()) leaveWith { onSearch(state.query.trim()) } }

    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(R.string.search_title), onBack = { leaveWith(onBack) }) {
                TopBarTextAction(text = stringResource(R.string.action_cancel), onClick = { leaveWith(onBack) })
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
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SearchInput(
                query = state.query,
                onQueryChange = onQueryChange,
                onSubmit = submit,
                modifier = Modifier.focusRequester(focusRequester),
            )
            if (state.query.isBlank()) {
                Text(
                    text = stringResource(R.string.search_type_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (state.suggestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.search_suggestions),
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 22.sp),
                    )
                    state.suggestions.forEach { suggestion ->
                        SuggestionRow(
                            title = suggestion.name,
                            subtitle = suggestion.area.orEmpty(),
                            onClick = { leaveWith { onSuggestion(suggestion) } },
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(
                    title = stringResource(R.string.search_recent),
                    titleStyle = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 20.sp),
                    actionLabel = stringResource(R.string.action_history),
                    onAction = { leaveWith(onHistory) },
                )
                state.recent.forEach { recent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button, onClick = { leaveWith { onSearch(recent) } })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_history),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(text = recent, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/** Focused search box with a clear button ("Ô tìm kiếm đang nhập" in Figma). */
@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                    .border(1.5.dp, colors.primary, MaterialTheme.shapes.small)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = colors.onSurface,
                    modifier = Modifier.size(20.dp),
                )
                Box(modifier = Modifier.weight(1f)) { innerTextField() }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(colors.outline)
                            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.cd_clear)) { onQueryChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_x),
                            contentDescription = stringResource(R.string.cd_clear),
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SuggestionRow(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(colors.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_map_pin),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
                )
                if (subtitle.isNotEmpty()) {
                    Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_up_left),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
        HorizontalDivider(thickness = 1.dp, color = colors.outline)
    }
}

@ThemePreviews
@Composable
private fun SearchScreenPreview() {
    TripFairTheme {
        SearchScreen(
            state = SearchUiState(
                query = "cầu",
                suggestions = listOf(SuggestionDto(1, "Cầu Vàng", "Hòa Vang, Đà Nẵng"), SuggestionDto(2, "Cầu Rồng", "Hải Châu, Đà Nẵng")),
                recent = listOf("Bãi biển Mỹ Khê"),
            ),
            onQueryChange = {},
            onBack = {},
            onSearch = {},
            onSuggestion = {},
            onHistory = {},
        )
    }
}
