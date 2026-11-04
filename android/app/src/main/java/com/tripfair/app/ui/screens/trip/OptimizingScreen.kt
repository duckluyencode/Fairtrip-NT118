package com.tripfair.app.ui.screens.trip

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.tripfair.app.R
import com.tripfair.app.ui.components.FillHeightScrollColumn
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import kotlinx.coroutines.delay

private val Steps = listOf(
    R.string.optimizing_step_tickets,
    R.string.optimizing_step_rooms,
    R.string.optimizing_step_pick,
    R.string.optimizing_step_fair,
)

/** Time each sample step takes; the real app waits for the backend instead. */
private const val STEP_MILLIS = 700L

/**
 * "Đang lập lộ trình": shows the optimizer's steps, then calls [onDone]. Without a backend the
 * steps are simulated; [animate] = false freezes them at [startStep] for previews.
 */
@Composable
fun OptimizingScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    startStep: Int = 0,
    animate: Boolean = true,
) {
    var step by rememberSaveable { mutableIntStateOf(startStep) }
    val currentOnDone by rememberUpdatedState(onDone)
    if (animate) {
        LaunchedEffect(Unit) {
            while (step < Steps.size) {
                delay(STEP_MILLIS)
                step++
            }
            delay(STEP_MILLIS / 2)
            currentOnDone()
        }
    }

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
                CircularProgressIndicator(
                    modifier = Modifier.size(96.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline,
                    strokeWidth = 10.dp,
                )
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
                    Steps.forEachIndexed { index, label ->
                        StepRow(label = stringResource(label), state = index.compareTo(step))
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
        OptimizingScreen(onBack = {}, onDone = {}, startStep = 2, animate = false)
    }
}
