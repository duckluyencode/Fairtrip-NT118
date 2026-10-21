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
import androidx.compose.runtime.Composable
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
import com.tripfair.app.R
import com.tripfair.app.ui.components.AvatarButton
import com.tripfair.app.ui.components.FillHeightScrollColumn
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairSwitch
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.components.formatVnd
import com.tripfair.app.ui.sample.SampleGroup
import com.tripfair.app.ui.sample.SampleTrip
import com.tripfair.app.ui.screens.group.CapsLabel
import com.tripfair.app.ui.screens.group.GroupHeading
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/**
 * "Tạo chuyến đi": destination, dates, your budget and whether to suggest hotels.
 * "Cùng nhóm" continues to the group setup; "Một mình" continues to choosing a hotel, or
 * straight to planning when hotel suggestions are off.
 */
@Composable
fun CreateTripScreen(
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onContinueGroup: () -> Unit,
    onChooseLodging: () -> Unit,
    onPlanWithoutLodging: () -> Unit,
    initiallyGroup: Boolean = true,
) {
    var group by rememberSaveable { mutableStateOf(initiallyGroup) }
    var destination by rememberSaveable { mutableStateOf(SampleTrip.DESTINATION) }
    var budget by rememberSaveable { mutableStateOf(SampleGroup.YOUR_BUDGET) }
    var suggestLodging by rememberSaveable { mutableStateOf(true) }

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
                    TripTypeSwitch(group = group, onChange = { group = it })
                }
                FormField(label = stringResource(R.string.trip_destination), icon = R.drawable.ic_map_pin) { style ->
                    BasicTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        singleLine = true,
                        textStyle = style,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // Picking dates is not designed yet, so the sample dates are shown as text.
                FormField(label = stringResource(R.string.trip_dates), icon = R.drawable.ic_calendar) { style ->
                    Text(text = SampleTrip.FORM_DATES, style = style)
                }
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    FormField(label = stringResource(R.string.trip_budget), icon = R.drawable.ic_wallet) { style ->
                        val text = formatVnd(budget.toLongOrNull() ?: 0L)
                        BasicTextField(
                            // Keeps the cursor before "đ", like the budget field of the group screens.
                            value = TextFieldValue(text, selection = TextRange(text.length - 1)),
                            onValueChange = { new -> budget = new.text.filter { it.isDigit() }.trimStart('0').take(12) },
                            singleLine = true,
                            textStyle = style,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        text = stringResource(if (group) R.string.trip_budget_helper_group else R.string.trip_budget_helper),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = stringResource(R.string.trip_lodging_title), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = stringResource(R.string.trip_lodging_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TripFairSwitch(checked = suggestLodging, onCheckedChange = { suggestLodging = it }, large = true)
                }
            }
            Box(modifier = Modifier.padding(top = 20.dp)) {
                when {
                    group -> PrimaryButton(text = stringResource(R.string.trip_continue_group), onClick = onContinueGroup, height = 52.dp)
                    suggestLodging -> PrimaryButton(text = stringResource(R.string.trip_continue_lodging), onClick = onChooseLodging, height = 52.dp)
                    else -> PrimaryButton(text = stringResource(R.string.trip_continue_plan), onClick = onPlanWithoutLodging, height = 52.dp)
                }
            }
        }
    }
}

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
private fun FormField(label: String, @DrawableRes icon: Int, value: @Composable (TextStyle) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        CapsLabel(text = label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                .border(1.dp, colors.outline, MaterialTheme.shapes.small)
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            value(MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, color = colors.onSurface))
        }
    }
}

@ThemePreviews
@Composable
private fun CreateTripScreenPreview() {
    TripFairTheme {
        CreateTripScreen(onBack = {}, onProfile = {}, onContinueGroup = {}, onChooseLodging = {}, onPlanWithoutLodging = {})
    }
}

@ThemePreviews
@Composable
private fun CreateSoloTripPreview() {
    TripFairTheme {
        CreateTripScreen(
            onBack = {},
            onProfile = {},
            onContinueGroup = {},
            onChooseLodging = {},
            onPlanWithoutLodging = {},
            initiallyGroup = false,
        )
    }
}
