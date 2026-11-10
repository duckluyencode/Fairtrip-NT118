package com.tripfair.app.ui.screens.trip

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.AvatarButton
import com.tripfair.app.ui.components.FillHeightScrollColumn
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairSwitch
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.components.formatVnd
import com.tripfair.app.ui.screens.group.CapsLabel
import com.tripfair.app.ui.screens.group.GroupHeading
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Composable
fun CreateTripRoute(
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onNext: (CreateTripNext) -> Unit,
    viewModel: CreateTripViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(viewModel.next) {
        viewModel.next?.let {
            onNext(it)
            viewModel.onNavigated()
        }
    }
    CreateTripScreen(
        state = viewModel.state,
        onBack = onBack,
        onProfile = onProfile,
        onGroupChange = viewModel::onGroupChange,
        onCityChange = viewModel::onCityChange,
        onDatesChange = viewModel::onDatesChange,
        onBudgetChange = viewModel::onBudgetChange,
        onLodgingChange = viewModel::onLodgingChange,
        onContinue = viewModel::submit,
    )
}

/**
 * "Tạo chuyến đi": destination, dates, your budget and whether to suggest hotels.
 * "Cùng nhóm" continues to naming the group; "Một mình" creates the trip, then continues to
 * choosing a hotel, or straight to planning when hotel suggestions are off.
 */
@Composable
fun CreateTripScreen(
    state: CreateTripUiState,
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onGroupChange: (Boolean) -> Unit,
    onCityChange: (String) -> Unit,
    onDatesChange: (LocalDate, LocalDate) -> Unit,
    onBudgetChange: (String) -> Unit,
    onLodgingChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var pickingDates by rememberSaveable { mutableStateOf(false) }

    if (pickingDates) {
        TripDatesDialog(
            start = state.startDate,
            end = state.endDate,
            onDismiss = { pickingDates = false },
            onConfirm = { start, end ->
                pickingDates = false
                onDatesChange(start, end)
            },
        )
    }

    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(R.string.trips_create), onBack = onBack) {
                AvatarButton(onClick = onProfile)
            }
        },
    ) { innerPadding ->
        FillHeightScrollColumn(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                GroupHeading(title = stringResource(R.string.trip_new_title), text = stringResource(R.string.trip_new_intro))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    CapsLabel(text = stringResource(R.string.trip_type))
                    TripTypeSwitch(group = state.group, onChange = onGroupChange)
                }
                FormField(
                    label = stringResource(R.string.trip_destination),
                    icon = R.drawable.ic_map_pin,
                    error = state.cityError?.let { stringResource(it) },
                ) { style ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        if (state.city.isEmpty()) {
                            Text(text = stringResource(R.string.trip_destination_hint), style = style.copy(color = colors.onSurfaceVariant))
                        }
                        BasicTextField(
                            value = state.city,
                            onValueChange = onCityChange,
                            singleLine = true,
                            textStyle = style,
                            cursorBrush = SolidColor(colors.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                FormField(
                    label = stringResource(R.string.trip_dates),
                    icon = R.drawable.ic_calendar,
                    error = state.datesError?.let { stringResource(it) },
                    onClick = { pickingDates = true },
                ) { style ->
                    val start = state.startDate
                    val end = state.endDate
                    if (start != null && end != null) {
                        val days = ChronoUnit.DAYS.between(start, end).toInt() + 1
                        Text(text = "${tripDateRange(start, end)} · ${tripLength(days)}", style = style)
                    } else {
                        Text(text = stringResource(R.string.trip_dates_pick), style = style.copy(color = colors.onSurfaceVariant))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    FormField(label = stringResource(R.string.trip_budget), icon = R.drawable.ic_wallet) { style ->
                        val text = formatVnd(state.budget.toLongOrNull() ?: 0L)
                        BasicTextField(
                            // Keeps the cursor before "đ", like the budget field of the group screens.
                            value = TextFieldValue(text, selection = TextRange(text.length - 1)),
                            onValueChange = { new -> onBudgetChange(new.text.filter { it.isDigit() }.trimStart('0').take(10)) },
                            singleLine = true,
                            textStyle = style,
                            cursorBrush = SolidColor(colors.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        text = stringResource(if (state.group) R.string.trip_budget_helper_group else R.string.trip_budget_helper),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainerLow, MaterialTheme.shapes.medium)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = stringResource(R.string.trip_lodging_title), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = stringResource(R.string.trip_lodging_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    TripFairSwitch(checked = state.suggestLodging, onCheckedChange = onLodgingChange, large = true)
                }
            }
            Column(modifier = Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.error?.let {
                    Text(text = stringResource(it), style = MaterialTheme.typography.bodySmall, color = colors.error)
                }
                val label = when {
                    state.group -> R.string.trip_continue_group
                    state.suggestLodging -> R.string.trip_continue_lodging
                    else -> R.string.trip_continue_plan
                }
                PrimaryButton(text = stringResource(label), onClick = onContinue, height = 52.dp, enabled = !state.loading)
            }
        }
    }
}

/** Picks the first and last day; days before today cannot be picked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDatesDialog(
    start: LocalDate?,
    end: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = start?.toUtcMillis(),
        initialSelectedEndDateMillis = end?.toUtcMillis(),
        yearRange = today.year..today.year + 1,
        selectableDates = remember(today) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = !utcTimeMillis.toLocalDate().isBefore(today)

                override fun isSelectableYear(year: Int) = year >= today.year
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val first = state.selectedStartDateMillis?.toLocalDate() ?: return@TextButton
                    onConfirm(first, state.selectedEndDateMillis?.toLocalDate() ?: first)
                },
                enabled = state.selectedStartDateMillis != null,
            ) { Text(stringResource(R.string.action_done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        val first = state.selectedStartDateMillis?.toLocalDate()
        val last = state.selectedEndDateMillis?.toLocalDate()
        DateRangePicker(
            state = state,
            modifier = Modifier.weight(1f),
            // The default headline ("Ngày bắt đầu - Ngày kết thúc") wraps at this width.
            title = null,
            headline = {
                Text(
                    text = when {
                        first != null && last != null -> tripDateRange(first, last)
                        first != null -> stringResource(R.string.trip_dates_pick_end, tripDateRange(first, first))
                        else -> stringResource(R.string.trip_dates_pick)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 12.dp),
                )
            },
            showModeToggle = false,
        )
    }
}

// The picker works in UTC midnight milliseconds.
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Two-option switch "Một mình" / "Cùng nhóm" ("Chọn kiểu" in Figma). */
@Composable
private fun TripTypeSwitch(group: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
            .border(1.dp, colors.outline, MaterialTheme.shapes.small)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(false to R.string.trip_type_solo, true to R.string.trip_type_group).forEach { (isGroup, label) ->
            val selected = group == isGroup
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) colors.primary else Color.Transparent)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onChange(isGroup) })
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                    color = if (selected) colors.onPrimary else colors.onSurface,
                )
            }
        }
    }
}

/** Caps label over a 56dp white box with a teal icon ("Trường thông tin" in Figma). */
@Composable
private fun FormField(
    label: String,
    @DrawableRes icon: Int,
    error: String? = null,
    onClick: (() -> Unit)? = null,
    value: @Composable (TextStyle) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        CapsLabel(text = label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(shape)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .background(colors.surfaceContainerLow, shape)
                .border(1.dp, if (error != null) colors.error else colors.outline, shape)
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            value(MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, color = colors.onSurface))
        }
        error?.let { Text(text = it, style = MaterialTheme.typography.labelSmall, color = colors.error) }
    }
}

@ThemePreviews
@Composable
private fun CreateTripScreenPreview() {
    TripFairTheme {
        CreateTripScreen(
            state = CreateTripUiState(
                city = "Đà Nẵng",
                startDate = LocalDate.of(2026, 12, 12),
                endDate = LocalDate.of(2026, 12, 14),
                budget = "2500000",
            ),
            onBack = {}, onProfile = {}, onGroupChange = {}, onCityChange = {}, onDatesChange = { _, _ -> },
            onBudgetChange = {}, onLodgingChange = {}, onContinue = {},
        )
    }
}

@ThemePreviews
@Composable
private fun CreateSoloTripPreview() {
    TripFairTheme {
        CreateTripScreen(
            state = CreateTripUiState(group = false, datesError = R.string.trip_error_dates_missing),
            onBack = {}, onProfile = {}, onGroupChange = {}, onCityChange = {}, onDatesChange = { _, _ -> },
            onBudgetChange = {}, onLodgingChange = {}, onContinue = {},
        )
    }
}
