package com.tripfair.app.ui.screens.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tripfair.app.R
import com.tripfair.app.ui.components.OutlineButton
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TopBarTextAction
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.ItineraryDay
import com.tripfair.app.ui.sample.ItineraryStop
import com.tripfair.app.ui.sample.SampleItinerary
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/**
 * "Tự thiết kế lộ trình": hold and drag a stop to reorder it, swipe it left to remove it.
 * The budget card updates as you edit and turns red when the trip goes over budget.
 */
@Composable
fun ItineraryEditorScreen(
    initialDays: List<ItineraryDay>,
    onBack: () -> Unit,
    onSave: (List<ItineraryDay>) -> Unit,
    onAddPlace: () -> Unit,
    onReoptimize: () -> Unit,
) {
    var days by remember { mutableStateOf(initialDays) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val dayIndex = selected.coerceIn(0, days.lastIndex)
    val day = days[dayIndex]

    fun setStops(stops: List<ItineraryStop>) {
        days = days.toMutableList().also { it[dayIndex] = it[dayIndex].copy(stops = stops) }
    }

    // Times belong to the slots of the day: a moved stop takes the time of its new slot.
    fun moveStop(from: Int, to: Int) {
        val stops = days[dayIndex].stops
        val moved = stops.toMutableList().apply { add(to, removeAt(from)) }
        setStops(moved.mapIndexed { index, stop -> stop.copy(time = stops[index].time) })
    }

    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(R.string.itinerary_edit), onBack = onBack) {
                TopBarTextAction(text = stringResource(R.string.editor_save), onClick = { onSave(days) })
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlineButton(text = stringResource(R.string.editor_reoptimize), onClick = onReoptimize, trailingIcon = R.drawable.ic_arrow_right)
                PrimaryButton(
                    text = stringResource(R.string.editor_save_itinerary),
                    onClick = { onSave(days) },
                    trailingIcon = R.drawable.ic_arrow_right,
                    height = 52.dp,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DayChips(days = days, selected = dayIndex, onSelect = { selected = it })
            DayHeader(day = day)
            TripBudgetCard(days = days, day = day, editing = true)
            Text(
                text = stringResource(R.string.editor_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EditableTimeline(
                    stops = day.stops,
                    startTotal = days.costBefore(dayIndex),
                    onMove = ::moveStop,
                    onRemove = { id -> setStops(days[dayIndex].stops.filterNot { it.id == id }) },
                )
                AddPlaceButton(onClick = onAddPlace)
            }
        }
    }
}

/** Timeline whose rows can be dragged after a long press and swiped left to delete. */
@Composable
private fun EditableTimeline(
    stops: List<ItineraryStop>,
    startTotal: Long,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (id: String) -> Unit,
) {
    val rowGap = 12.dp
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<String, Int>() }
    // The drag gesture outlives recompositions, so it reads the latest list and callback.
    val currentStops by rememberUpdatedState(stops)
    val currentOnMove by rememberUpdatedState(onMove)

    Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
        var total = startTotal
        stops.forEach { stop ->
            total += stop.cost
            val runningTotal = total
            key(stop.id) {
                val dismissState = rememberSwipeToDismissBoxState()
                val isDragging = draggingId == stop.id
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onRemove(stop.id) },
                    backgroundContent = { DeleteBackground() },
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffset else 0f
                            shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                        }
                        .onSizeChanged { heights[stop.id] = it.height }
                        .pointerInput(stop.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = stop.id
                                    dragOffset = 0f
                                },
                                onDragEnd = { draggingId = null },
                                onDragCancel = { draggingId = null },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val list = currentStops
                                    val index = list.indexOfFirst { it.id == stop.id }
                                    val gap = rowGap.toPx()
                                    // Swap with a neighbour once the row passes half of it.
                                    if (dragOffset > 0 && index < list.lastIndex) {
                                        val next = (heights[list[index + 1].id] ?: 0) + gap
                                        if (dragOffset > next / 2) {
                                            currentOnMove(index, index + 1)
                                            dragOffset -= next
                                        }
                                    } else if (dragOffset < 0 && index > 0) {
                                        val previous = (heights[list[index - 1].id] ?: 0) + gap
                                        if (-dragOffset > previous / 2) {
                                            currentOnMove(index, index - 1)
                                            dragOffset += previous
                                        }
                                    }
                                },
                            )
                        },
                ) {
                    TimelineRow(
                        stop = stop,
                        runningTotal = runningTotal,
                        modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    )
                }
            }
        }
    }
}

/** Red strip revealed while swiping a stop to the left. */
@Composable
private fun DeleteBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = stringResource(R.string.cd_delete_stop),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

/** Dashed "+ Thêm địa điểm" button under the timeline. */
@Composable
private fun AddPlaceButton(onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = color,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.editor_add_place),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

@ThemePreviews
@Composable
private fun ItineraryEditorScreenPreview() {
    TripFairTheme {
        ItineraryEditorScreen(
            initialDays = SampleItinerary.editDraft(SampleItinerary.days),
            onBack = {},
            onSave = {},
            onAddPlace = {},
            onReoptimize = {},
        )
    }
}
