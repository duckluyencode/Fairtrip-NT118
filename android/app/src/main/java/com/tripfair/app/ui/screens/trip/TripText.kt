package com.tripfair.app.ui.screens.trip

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tripfair.app.R
import com.tripfair.app.data.network.ApiError
import com.tripfair.app.data.trips.TripSummaryDto
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** "12 – 14 tháng 10", "30 tháng 9 – 2 tháng 10" or "12 tháng 10". */
@Composable
fun tripDateRange(start: LocalDate, end: LocalDate): String = when {
    start == end -> stringResource(R.string.trip_date_single, start.dayOfMonth, start.monthValue)
    start.year == end.year && start.month == end.month ->
        stringResource(R.string.trip_date_range_month, start.dayOfMonth, end.dayOfMonth, end.monthValue)
    else -> stringResource(R.string.trip_date_range, start.dayOfMonth, start.monthValue, end.dayOfMonth, end.monthValue)
}

/** "3 ngày 2 đêm", or "1 ngày". */
@Composable
fun tripLength(days: Int): String =
    if (days > 1) stringResource(R.string.trip_length_nights, days, days - 1) else stringResource(R.string.trip_length_day)

/** "12 – 14 tháng 10 · 3 ngày 2 đêm" */
@Composable
fun tripDatesLine(trip: TripSummaryDto): String =
    "${tripDateRange(LocalDate.parse(trip.startDate), LocalDate.parse(trip.endDate))} · ${tripLength(trip.days)}"

/** "20:00 12/12", in the phone's time zone; the text as it is when it is not a date. */
fun voteDeadlineText(iso: String): String = try {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm d/M"))
} catch (_: DateTimeParseException) {
    iso
}

/** A group's name, or the city of a solo trip. */
fun TripSummaryDto.title(): String = name ?: city

/** The trip to show on the home screen: the next one to start, else the most recent. */
fun List<TripSummaryDto>.upcoming(today: LocalDate = LocalDate.now()): TripSummaryDto? =
    filter { !LocalDate.parse(it.endDate).isBefore(today) }.minByOrNull { it.startDate }
        ?: maxByOrNull { it.startDate }

/** Messages for the backend's trip error codes. */
@StringRes
fun tripErrorMessage(error: ApiError): Int = when (error.code) {
    ApiError.NETWORK -> R.string.error_network
    "TRIP_NOT_FOUND" -> R.string.trip_error_not_found
    "NOT_TRIP_OWNER" -> R.string.trip_error_not_owner
    "INVALID_TRIP_DATES" -> R.string.trip_error_dates
    "INVITE_CODE_NOT_FOUND" -> R.string.trip_error_code_not_found
    "ALREADY_MEMBER" -> R.string.trip_error_already_member
    "HOTEL_NOT_FOUND" -> R.string.trip_error_hotel_not_found
    "LODGING_OVER_BUDGET" -> R.string.trip_error_lodging_over_budget
    "LODGING_NOT_CHOSEN" -> R.string.trip_error_lodging_not_chosen
    "OPTION_NOT_FOUND" -> R.string.trip_error_option_not_found
    "VOTING_CLOSED" -> R.string.trip_error_voting_closed
    "BALLOT_ALREADY_SENT" -> R.string.trip_error_ballot_sent
    "NO_PLACES_TO_PLAN" -> R.string.trip_error_no_places
    "INVALID_ITINERARY" -> R.string.trip_error_invalid_itinerary
    "ITINERARY_NOT_READY" -> R.string.trip_error_itinerary_not_ready
    "GOOGLE_PLACES_UNAVAILABLE" -> R.string.error_google_unavailable
    else -> R.string.error_unexpected
}
