package com.tripfair.app.ui.screens.trip

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
import com.tripfair.app.ui.components.AvatarButton
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.ProgressBar
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.components.formatVnd
import com.tripfair.app.ui.sample.CostLine
import com.tripfair.app.ui.sample.MemberResult
import com.tripfair.app.ui.sample.SampleItinerary
import com.tripfair.app.ui.sample.SampleOptimization
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import com.tripfair.app.ui.theme.extendedColors

/**
 * "Kết quả tối ưu công bằng": overall satisfaction, each member's spending and satisfaction,
 * and the cost per place. Only you see how much of your own budget is left.
 */
@Composable
fun OptimizationResultScreen(onBack: () -> Unit, onProfile: () -> Unit, onViewItinerary: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(R.string.result_title), onBack = onBack) {
                AvatarButton(onClick = onProfile)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primary, MaterialTheme.shapes.large)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.result_caps),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.primaryContainer,
                )
                Text(
                    text = stringResource(R.string.result_satisfaction, SampleOptimization.SATISFACTION),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.onPrimary,
                )
                Text(text = SampleOptimization.SUMMARY, style = MaterialTheme.typography.bodyMedium, color = colors.primaryContainer)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primaryContainer, MaterialTheme.shapes.small)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val success = MaterialTheme.extendedColors.success
                Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null, tint = success, modifier = Modifier.size(18.dp))
                Text(text = stringResource(R.string.result_no_overspend), style = MaterialTheme.typography.labelMedium, color = success)
            }
            Text(text = stringResource(R.string.result_split), style = MaterialTheme.typography.titleMedium)
            SampleOptimization.members.forEach { MemberResultCard(it) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.tertiaryContainer, MaterialTheme.shapes.small)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.result_group_total),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatVnd(SampleOptimization.members.sumOf { it.spent }),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                )
            }
            Text(text = stringResource(R.string.result_privacy), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.result_costs), style = MaterialTheme.typography.titleMedium)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SampleOptimization.costs.forEach { CostRow(it) }
                }
            }
            PrimaryButton(text = stringResource(R.string.result_view_itinerary), onClick = onViewItinerary, height = 52.dp)
        }
    }
}

@Composable
private fun MemberResultCard(member: MemberResult) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (member.isYou) stringResource(R.string.member_you, member.name) else member.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.result_member_line, formatVnd(member.spent), member.satisfaction),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        ProgressBar(fraction = member.satisfaction / 100f, height = 7.dp)
        if (member.isYou) {
            // Budgets stay private, so only your own remaining amount is shown.
            Text(
                text = stringResource(R.string.result_you_left, formatVnd(SampleItinerary.BUDGET - member.spent)),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun CostRow(line: CostLine) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = line.name, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 18.sp))
            Text(text = line.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = line.price, style = MaterialTheme.typography.labelMedium)
    }
}

@ThemePreviews
@Composable
private fun OptimizationResultScreenPreview() {
    TripFairTheme {
        OptimizationResultScreen(onBack = {}, onProfile = {}, onViewItinerary = {})
    }
}
