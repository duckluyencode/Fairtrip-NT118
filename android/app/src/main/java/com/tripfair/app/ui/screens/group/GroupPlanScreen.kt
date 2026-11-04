package com.tripfair.app.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.OutlineButton
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TopBarTextAction
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleGroup
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/** "Lập kế hoạch nhóm": members' readiness, budgets (own only) and the vote kickoff. */
@Composable
fun GroupPlanScreen(
    onBack: () -> Unit,
    onInvite: () -> Unit,
    onEditBudget: () -> Unit,
    onStartVote: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TripFairTopBar(title = SampleGroup.NAME, onBack = onBack) {
                TopBarTextAction(text = stringResource(R.string.create_group_invite), onClick = onInvite)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primaryContainer, MaterialTheme.shapes.small)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(painter = painterResource(R.drawable.ic_person_tick), contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(text = stringResource(R.string.plan_ready_title), style = MaterialTheme.typography.bodyLarge, color = colors.primary)
                    Text(text = stringResource(R.string.plan_ready_text), style = MaterialTheme.typography.bodySmall, color = colors.primary)
                }
            }
            Text(
                text = stringResource(R.string.groups_budgets),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, letterSpacing = 0.sp),
            )
            SampleGroup.members.forEachIndexed { index, member ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MemberAvatar(index = index, size = 44.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (member.isYou) stringResource(R.string.member_you, member.name) else member.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(text = member.preferences, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    // Only your own budget is shown; others stay private.
                    MemberBadge(text = member.budget ?: stringResource(R.string.plan_budget_set), large = true)
                }
            }
            FairnessNote(text = stringResource(R.string.plan_fairness_note))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(R.string.plan_vote_deadline), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.plan_vote_deadline_value, SampleGroup.VOTE_DEADLINE),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.primary,
                )
            }
            OutlineButton(text = stringResource(R.string.plan_edit_budget), onClick = onEditBudget, trailingIcon = R.drawable.ic_arrow_right)
            PrimaryButton(text = stringResource(R.string.plan_start_vote), onClick = onStartVote, height = 52.dp)
        }
    }
}

@ThemePreviews
@Composable
private fun GroupPlanScreenPreview() {
    TripFairTheme {
        GroupPlanScreen(onBack = {}, onInvite = {}, onEditBudget = {}, onStartVote = {})
    }
}
