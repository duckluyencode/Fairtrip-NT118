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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.trips.ItineraryDto
import com.tripfair.app.data.trips.ItineraryStopDto
import com.tripfair.app.data.trips.MemberCostDto
import com.tripfair.app.data.trips.TripDetailDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.AvatarButton
import com.tripfair.app.ui.components.ErrorState
import com.tripfair.app.ui.components.LoadingState
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.ProgressBar
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.components.formatVnd
import com.tripfair.app.ui.sample.SampleTripPlan
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import com.tripfair.app.ui.theme.extendedColors
import java.util.Locale

@Composable
fun OptimizationResultRoute(
    tripId: Int,
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onViewItinerary: () -> Unit,
    viewModel: ItineraryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(tripId) { viewModel.start(tripId) }
    val itinerary = viewModel.itinerary
    val trip = viewModel.trip
    if (itinerary == null || trip == null) {
        TripLoadScaffold(
            title = stringResource(R.string.result_title),
            loading = viewModel.loading,
            error = viewModel.error?.let { tripErrorMessage(it) },
            onRetry = viewModel::load,
            onBack = onBack,
        )
    } else {
        OptimizationResultScreen(trip = trip, itinerary = itinerary, onBack = onBack, onProfile = onProfile, onViewItinerary = onViewItinerary)
    }
}

/** A screen still loading its trip, or showing why it could not. */
@Composable
internal fun TripLoadScaffold(
    title: String,
    loading: Boolean,
    error: Int?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onPlan: (() -> Unit)? = null,
) {
    Scaffold(topBar = { TripFairTopBar(title = title, onBack = onBack) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(horizontal = 20.dp)) {
            when {
                loading -> LoadingState()
                // No itinerary yet: planning it is the way forward, not retrying.
                error != null && onPlan != null -> Column(
                    modifier = Modifier.padding(vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(text = stringResource(error), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryButton(text = stringResource(R.string.trip_continue_plan), onClick = onPlan, height = 52.dp)
                }
                error != null -> ErrorState(message = stringResource(error), onRetry = onRetry)
            }
        }
    }
}

/**
 * "Kết quả tối ưu công bằng": overall satisfaction, each member's spending and satisfaction,
 * and the cost per place. Only you see how much of your own budget is left.
 */
@Composable
fun OptimizationResultScreen(
    trip: TripDetailDto,
    itinerary: ItineraryDto,
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onViewItinerary: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val stops = itinerary.days.flatMap { it.stops }
    val placeCount = stops.distinctBy { it.placeId }.size
    val satisfaction = itinerary.members.mapNotNull { it.satisfaction }.takeIf { it.isNotEmpty() }?.average()?.toInt()
    val overspent = itinerary.warnings.any { it.code == "OVER_BUDGET" || it.code == "MEMBER_OVER_BUDGET" }
    val myId = trip.me?.memberId
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
                satisfaction?.let {
                    Text(
                        text = stringResource(R.string.result_satisfaction, it),
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.onPrimary,
                    )
                }
                Text(
                    text = stringResource(
                        if (itinerary.lodging != null) R.string.result_summary_lodging else R.string.result_summary,
                        placeCount,
                        trip.days,
                        trip.memberCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primaryContainer,
                )
                itinerary.fairness?.let {
                    Text(
                        text = stringResource(R.string.result_fairness, String.format(Locale.forLanguageTag("vi"), "%.2f", it)),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primaryContainer,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (overspent) colors.errorContainer else colors.primaryContainer, MaterialTheme.shapes.small)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val tint = if (overspent) colors.error else MaterialTheme.extendedColors.success
                Icon(
                    painter = painterResource(if (overspent) R.drawable.ic_circle_alert else R.drawable.ic_check),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(if (overspent) R.string.result_overspend else R.string.result_no_overspend),
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                )
            }
            Text(text = stringResource(R.string.result_split), style = MaterialTheme.typography.titleMedium)
            itinerary.members.forEach { member ->
                MemberResultCard(member = member, isYou = member.memberId == myId, remaining = itinerary.me.remaining)
            }
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
                    text = formatVnd(itinerary.members.sumOf { it.spent }),
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
                    itinerary.lodging?.let { lodging ->
                        CostRow(
                            name = stringResource(R.string.result_lodging_line, lodging.name ?: stringResource(R.string.itinerary_lodging_fallback), lodging.nights),
                            detail = stringResource(R.string.result_lodging_detail, lodging.rooms, priceSourceLabel(lodging.priceSource, lodging.provider)),
                            price = formatVnd(lodging.myShare).let { if (lodging.isEstimate) "~$it" else it },
                        )
                    }
                    stops.distinctBy { it.placeId }.forEach { stop ->
                        CostRow(
                            name = stop.name ?: stringResource(R.string.itinerary_place_fallback),
                            detail = "${who(stop, trip.memberCount)} · ${priceSourceLabel(stop.price.source, stop.price.provider)}",
                            price = stopPrice(stop),
                        )
                    }
                }
            }
            PrimaryButton(text = stringResource(R.string.result_view_itinerary_days, trip.days), onClick = onViewItinerary, height = 52.dp)
        }
    }
}

@Composable
private fun who(stop: ItineraryStopDto, memberCount: Int): String =
    if (stop.participants.size >= memberCount) {
        stringResource(R.string.result_cost_everyone)
    } else {
        stop.participants.joinToString(", ") { it.fullName }
    }

@Composable
private fun stopPrice(stop: ItineraryStopDto): String = when {
    stop.price.perPerson == 0L -> stringResource(R.string.itinerary_free)
    stop.price.isEstimate -> "~" + formatVnd(stop.price.perPerson)
    else -> formatVnd(stop.price.perPerson)
}

@Composable
private fun MemberResultCard(member: MemberCostDto, isYou: Boolean, remaining: Long) {
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
                text = if (isYou) stringResource(R.string.member_you, member.fullName) else member.fullName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = member.satisfaction?.let { stringResource(R.string.result_member_line, formatVnd(member.spent), it) } ?: formatVnd(member.spent),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        member.satisfaction?.let { ProgressBar(fraction = it / 100f, height = 7.dp) }
        if (isYou) {
            // Budgets stay private, so only your own remaining amount is shown.
            Text(
                text = stringResource(R.string.result_you_left, formatVnd(remaining)),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun CostRow(name: String, detail: String, price: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 18.sp))
            Text(text = detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = price, style = MaterialTheme.typography.labelMedium)
    }
}

@ThemePreviews
@Composable
private fun OptimizationResultScreenPreview() {
    TripFairTheme {
        OptimizationResultScreen(trip = SampleTripPlan.trip, itinerary = SampleTripPlan.itinerary, onBack = {}, onProfile = {}, onViewItinerary = {})
    }
}
