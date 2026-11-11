package com.tripfair.app.ui.screens.group

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.trips.TripDraft
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.ListCard
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.components.formatVnd
import com.tripfair.app.ui.screens.trip.tripDateRange
import com.tripfair.app.ui.screens.trip.tripLength
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import java.time.LocalDate

@Composable
fun CreateGroupRoute(
    draft: TripDraft,
    ownerName: String,
    onBack: () -> Unit,
    onCreated: (CreatedGroup) -> Unit,
    viewModel: CreateGroupViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(viewModel.created) {
        viewModel.created?.let {
            onCreated(it)
            viewModel.onNavigated()
        }
    }
    CreateGroupScreen(
        draft = draft,
        ownerName = ownerName,
        name = viewModel.name,
        onNameChange = viewModel::onNameChange,
        nameError = viewModel.nameError,
        error = viewModel.error,
        loading = viewModel.loading,
        onBack = onBack,
        onInvite = { viewModel.create(draft, invite = true) },
        onContinue = { viewModel.create(draft, invite = false) },
    )
}

/**
 * "Tạo nhóm": the group's name, with the creator as its first member. The group is created
 * when the user continues or invites friends, since the invite code comes from the backend.
 */
@Composable
fun CreateGroupScreen(
    draft: TripDraft,
    ownerName: String,
    name: String,
    onNameChange: (String) -> Unit,
    @StringRes nameError: Int?,
    @StringRes error: Int?,
    loading: Boolean,
    onBack: () -> Unit,
    onInvite: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val destination = "${draft.city} · ${tripDateRange(LocalDate.parse(draft.startDate), LocalDate.parse(draft.endDate))} · ${tripLength(draft.days)}"

    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.group_create), onBack = onBack) },
        bottomBar = {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                error?.let { Text(text = stringResource(it), style = MaterialTheme.typography.bodySmall, color = colors.error) }
                PrimaryButton(text = stringResource(R.string.create_group_continue), onClick = onContinue, height = 52.dp, enabled = !loading)
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
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            GroupHeading(
                title = stringResource(R.string.create_group_heading),
                text = stringResource(R.string.create_group_intro),
                extra = destination,
            )
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                CapsLabel(text = stringResource(R.string.create_group_name_label))
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                                .border(1.dp, if (nameError != null) colors.error else colors.outline, MaterialTheme.shapes.small)
                                .padding(horizontal = 15.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (name.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.create_group_name_hint),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
                nameError?.let { Text(text = stringResource(it), style = MaterialTheme.typography.labelSmall, color = colors.error) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.create_group_members),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, letterSpacing = 0.sp),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = pluralStringResource(R.plurals.groups_member_count, 1, 1),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary,
                    )
                }
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.primaryContainer)
                        .clickable(role = Role.Button, enabled = !loading, onClick = onInvite)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(painter = painterResource(R.drawable.ic_plus), contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                    Text(
                        text = stringResource(R.string.create_group_invite),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.primary,
                    )
                }
                ListCard {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MemberAvatar(index = 0, size = 36.dp, initial = ownerName.trim().split(" ").lastOrNull()?.take(1))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = stringResource(R.string.member_you, ownerName), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(R.string.create_group_owner_budget, formatVnd(draft.budget)),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        MemberBadge(text = stringResource(R.string.member_leader))
                    }
                }
            }
            GroupHint(text = stringResource(R.string.create_group_hint_new))
        }
    }
}

@ThemePreviews
@Composable
private fun CreateGroupScreenPreview() {
    TripFairTheme {
        CreateGroupScreen(
            draft = TripDraft("Đà Nẵng", "2026-12-12", "2026-12-14", days = 3, budget = 2_500_000, hotel = true),
            ownerName = "Duy Đức",
            name = "Nhóm Đà Nẵng",
            onNameChange = {},
            nameError = null,
            error = null,
            loading = false,
            onBack = {},
            onInvite = {},
            onContinue = {},
        )
    }
}
