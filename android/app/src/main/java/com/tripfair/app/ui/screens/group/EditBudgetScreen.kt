package com.tripfair.app.ui.screens.group

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tripfair.app.data.trips.Interest
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.BudgetField
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/**
 * Edits your budget and interests in the trip [tripId], or, when [inviteCode] is given, joins
 * that group with them. [onSaved] gets the trip that was saved to or joined.
 */
@Composable
fun EditBudgetRoute(
    tripId: Int?,
    inviteCode: String?,
    memberName: String,
    onBack: () -> Unit,
    onSaved: (tripId: Int) -> Unit,
    viewModel: EditBudgetViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(tripId, inviteCode) {
        if (inviteCode != null) viewModel.startJoin(inviteCode) else if (tripId != null) viewModel.startEdit(tripId)
    }
    LaunchedEffect(viewModel.savedTripId) {
        viewModel.savedTripId?.let {
            onSaved(it)
            viewModel.onNavigated()
        }
    }
    EditBudgetScreen(
        memberName = memberName,
        groupLine = viewModel.groupName ?: inviteCode?.let { stringResource(R.string.budget_group_code, it) }.orEmpty(),
        joining = inviteCode != null,
        budget = viewModel.budget,
        interests = viewModel.interests,
        busy = viewModel.loading || viewModel.saving,
        error = viewModel.error,
        onBudgetChange = viewModel::onBudgetChange,
        onToggleInterest = viewModel::toggle,
        onBack = onBack,
        onSave = viewModel::save,
    )
}

@Composable
fun EditBudgetScreen(
    memberName: String,
    groupLine: String,
    joining: Boolean,
    budget: String,
    interests: Set<String>,
    busy: Boolean,
    @StringRes error: Int?,
    onBudgetChange: (String) -> Unit,
    onToggleInterest: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.budget_title), onBack = onBack) },
        bottomBar = {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                error?.let { Text(text = stringResource(it), style = MaterialTheme.typography.bodySmall, color = colors.error) }
                PrimaryButton(
                    text = stringResource(if (joining) R.string.group_join else R.string.edit_profile_save),
                    onClick = onSave,
                    trailingIcon = R.drawable.ic_arrow_right,
                    enabled = !busy,
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
                .padding(start = 20.dp, end = 20.dp, top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MemberAvatar(index = 0, size = 40.dp, initial = memberName.trim().split(" ").lastOrNull()?.take(1))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.budget_member, memberName),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    if (groupLine.isNotEmpty()) {
                        Text(text = groupLine, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                Icon(painter = painterResource(R.drawable.ic_person_tick), contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(18.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = stringResource(R.string.budget_label), style = MaterialTheme.typography.titleSmall)
                BudgetField(digits = budget, onDigitsChange = onBudgetChange)
                Text(text = stringResource(R.string.budget_helper), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.budget_preferences), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(
                            text = pluralStringResource(R.plurals.budget_preferences_selected, interests.size, interests.size),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primary,
                        )
                    }
                    Text(text = stringResource(R.string.budget_preferences_intro), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                interestOptions.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { option ->
                            PreferenceOption(
                                option = option,
                                selected = option.code in interests,
                                onToggle = { onToggleInterest(option.code) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            FairnessNote(text = stringResource(R.string.budget_fairness_note))
            Box(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PreferenceOption(
    option: InterestOption,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(if (selected) colors.primaryContainer else colors.surfaceContainerLow)
            .border(1.dp, if (selected) colors.primary else colors.outline, shape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(option.icon),
            contentDescription = null,
            tint = if (selected) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(option.label),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp),
            color = if (selected) colors.primary else colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(if (selected) R.drawable.ic_check else R.drawable.ic_unchecked),
            contentDescription = null,
            tint = if (selected) colors.primary else colors.outline,
            modifier = Modifier.size(14.dp),
        )
    }
}

@ThemePreviews
@Composable
private fun EditBudgetScreenPreview() {
    TripFairTheme {
        EditBudgetScreen(
            memberName = "Duy Đức",
            groupLine = "Nhóm Đà Nẵng",
            joining = false,
            budget = "2500000",
            interests = setOf(Interest.FOOD, Interest.BEACH),
            busy = false,
            error = null,
            onBudgetChange = {},
            onToggleInterest = {},
            onBack = {},
            onSave = {},
        )
    }
}
