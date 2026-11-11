package com.tripfair.app.ui.screens.trip

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.FillHeightScrollColumn
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

private val Steps = listOf(
    R.string.optimizing_step_tickets,
    R.string.optimizing_step_rooms,
    R.string.optimizing_step_pick,
    R.string.optimizing_step_fair,
)

@Composable
fun OptimizingRoute(
    tripId: Int,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onChooseLodging: () -> Unit,
    viewModel: OptimizingViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(tripId) { viewModel.start(tripId) }
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(viewModel.done) {
        if (viewModel.done) currentOnDone()
    }
    val error = viewModel.error
    OptimizingScreen(
        onBack = onBack,
        step = viewModel.step,
        error = error?.let { tripErrorMessage(it) },
        needsLodging = error?.code == "LODGING_NOT_CHOSEN",
        onRetry = viewModel::plan,
        onChooseLodging = onChooseLodging,
    )
}

/**
 * "Đang lập lộ trình": the optimizer's steps while the backend plans the trip. When planning
 * fails the reason replaces the steps, with a retry (or "Chọn chỗ ở" when no hotel is chosen).
 */
@Composable
fun OptimizingScreen(
    onBack: () -> Unit,
    step: Int,
    @StringRes error: Int? = null,
    needsLodging: Boolean = false,
    onRetry: () -> Unit = {},
    onChooseLodging: () -> Unit = {},
) {
    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.optimizing_title), onBack = onBack) },
    ) { innerPadding ->
        FillHeightScrollColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                if (error == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(96.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline,
                        strokeWidth = 10.dp,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.optimizing_heading),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp, lineHeight = 27.sp),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.optimizing_time),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (error == null) {
                        Steps.forEachIndexed { index, label ->
                            StepRow(label = stringResource(label), state = index.compareTo(step))
                        }
                    } else {
                        Text(text = stringResource(R.string.optimizing_failed), style = MaterialTheme.typography.titleSmall)
                        Text(text = stringResource(error), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        if (needsLodging) {
                            PrimaryButton(text = stringResource(R.string.lodging_title), onClick = onChooseLodging, height = 52.dp)
                        } else {
                            PrimaryButton(text = stringResource(R.string.action_retry), onClick = onRetry, height = 52.dp)
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.padding(top = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = stringResource(R.string.optimizing_leave),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.action_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable(role = Role.Button, onClick = onBack)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** One step; [state] < 0 is done, 0 is running, > 0 is waiting. */
@Composable
private fun StepRow(label: String, state: Int) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .then(
                    when {
                        state < 0 -> Modifier.background(colors.primary, CircleShape)
                        state == 0 -> Modifier.border(2.dp, colors.primary, CircleShape)
                        else -> Modifier.border(1.5.dp, colors.outline, CircleShape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (state < 0) {
                Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(12.dp))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (state == 0) FontWeight.Bold else FontWeight.Normal),
            color = if (state > 0) colors.onSurfaceVariant else colors.onSurface,
        )
    }
}

@ThemePreviews
@Composable
private fun OptimizingScreenPreview() {
    TripFairTheme {
        OptimizingScreen(onBack = {}, step = 2)
    }
}
