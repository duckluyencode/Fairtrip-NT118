package com.tripfair.app.navigation

import androidx.navigation3.runtime.NavKey
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
data class PlaceDetail(val placeId: String) : NavKey

@Serializable
data class PlaceReviews(val placeId: String) : NavKey

// 08 Profile & favorites

@Serializable
data object EditProfile : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data object Support : NavKey

@Serializable
data object PrivacyPolicyPage : NavKey

@Serializable
data object TermsPage : NavKey

// 03 Groups & members

@Serializable
data object GroupChoice : NavKey

@Serializable
data object CreateGroup : NavKey

@Serializable
data object JoinGroup : NavKey

@Serializable
data object InviteFriends : NavKey

@Serializable
data object GroupPlan : NavKey

@Serializable
data object EditBudget : NavKey

// 04 Votes

@Serializable
data class AddToGroupVote(val placeId: String) : NavKey

@Serializable
data object VoteList : NavKey

@Serializable
data object Ballot : NavKey

// 05 Trip & optimization, 10 Solo trip & lodging

@Serializable
data object CreateTrip : NavKey

/** [group] decides where the optimizer goes next: the fairness result or the itinerary. */
@Serializable
data class LodgingChoice(val group: Boolean) : NavKey

@Serializable
data class Optimizing(val group: Boolean) : NavKey

@Serializable
data object OptimizationResult : NavKey

@Serializable
data object DayItinerary : NavKey

@Serializable
data object ItineraryEditor : NavKey

// Screens of later groups, reached from the ones above

@Serializable
data object EmergencyContacts : NavKey

@Serializable
data object Sos : NavKey

@Serializable
data object CameraScan : NavKey
