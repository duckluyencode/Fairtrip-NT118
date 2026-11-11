package com.tripfair.app.navigation

import androidx.navigation3.runtime.NavKey
import com.tripfair.app.data.trips.TripDraft
import kotlinx.serialization.Serializable

// One route per screen. @Serializable lets the back stack survive rotation and process death.

// 01 Authentication

@Serializable
data object Login : NavKey

@Serializable
data object Register : NavKey

@Serializable
data object ForgotPassword : NavKey

/** Why the code was sent: decides where "Xác thực mã" goes next. */
@Serializable
enum class OtpPurpose { Register, ResetPassword }

@Serializable
data class OtpVerification(val purpose: OtpPurpose) : NavKey

@Serializable
data object NewPassword : NavKey

// Main tabs

@Serializable
data object Home : NavKey

@Serializable
data object Trips : NavKey

@Serializable
data object Favorites : NavKey

@Serializable
data object Profile : NavKey

// 02 Explore & search

@Serializable
data object Search : NavKey

@Serializable
data class SearchResults(val query: String) : NavKey

@Serializable
data object SearchHistory : NavKey

@Serializable
data class PlaceDetail(val placeId: String, val sessionToken: String? = null) : NavKey

@Serializable
data class PlaceReviews(val placeId: String) : NavKey

// 08 Profile & favorites

@Serializable
data object EditProfile : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data object DeleteAccount : NavKey

@Serializable
data object Support : NavKey

@Serializable
data object PrivacyPolicyPage : NavKey

@Serializable
data object TermsPage : NavKey

// 03 Groups & members

@Serializable
data object GroupChoice : NavKey

/** Names the group of a trip drafted in "Tạo chuyến đi". */
@Serializable
data class CreateGroup(val draft: TripDraft) : NavKey

@Serializable
data object JoinGroup : NavKey

/** Last step of joining: the budget and interests sent with [inviteCode]. */
@Serializable
data class JoinGroupBudget(val inviteCode: String) : NavKey

@Serializable
data class InviteFriends(val tripId: Int) : NavKey

@Serializable
data class GroupPlan(val tripId: Int) : NavKey

@Serializable
data class EditBudget(val tripId: Int) : NavKey

// 04 Votes

@Serializable
data class AddToGroupVote(val placeId: String) : NavKey

@Serializable
data class VoteList(val tripId: Int) : NavKey

@Serializable
data class Ballot(val tripId: Int) : NavKey

// 05 Trip & optimization, 10 Solo trip & lodging

@Serializable
data object CreateTrip : NavKey

/** [group] decides where the optimizer goes next: the fairness result or the itinerary. */
@Serializable
data class LodgingChoice(val tripId: Int, val group: Boolean) : NavKey

@Serializable
data class Optimizing(val tripId: Int, val group: Boolean) : NavKey

@Serializable
data class OptimizationResult(val tripId: Int) : NavKey

@Serializable
data class DayItinerary(val tripId: Int) : NavKey

@Serializable
data class ItineraryEditor(val tripId: Int) : NavKey

/** Search used to pick a place for the itinerary being edited. */
@Serializable
data object PickPlace : NavKey

// 06 Camera & SOS

@Serializable
data object CameraScan : NavKey

@Serializable
data object RecognitionResult : NavKey

@Serializable
data object Sos : NavKey

@Serializable
data object EmergencyContacts : NavKey

/** Adds a contact, or edits the one with [contactId]. */
@Serializable
data class ContactForm(val contactId: Int? = null) : NavKey
