package com.tripfair.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.tripfair.app.R
import com.tripfair.app.ui.components.MainTab
import com.tripfair.app.ui.sample.SampleData
import com.tripfair.app.ui.sample.SamplePlaces
import com.tripfair.app.ui.sample.SampleSearch
import com.tripfair.app.ui.screens.PlaceholderScreen
import com.tripfair.app.ui.screens.auth.ForgotPasswordScreen
import com.tripfair.app.ui.screens.auth.LoginScreen
import com.tripfair.app.ui.screens.auth.NewPasswordScreen
import com.tripfair.app.ui.screens.auth.OtpVerificationScreen
import com.tripfair.app.ui.screens.auth.RegisterScreen
import com.tripfair.app.ui.screens.explore.PlaceDetailScreen
import com.tripfair.app.ui.screens.explore.PlaceReviewsScreen
import com.tripfair.app.ui.screens.explore.SearchHistoryScreen
import com.tripfair.app.ui.screens.explore.SearchResultsScreen
import com.tripfair.app.ui.screens.explore.SearchScreen
import com.tripfair.app.ui.screens.home.HomeScreen

/**
 * Connects every button to its target screen, following the flow notes in Figma.
 * The back stack is a plain list: navigating adds a route, going back removes the last one.
 */
@Composable
fun TripFairNavDisplay() {
    val backStack = rememberNavBackStack(Login)

    // Switching tabs starts a fresh history at that tab's root screen.
    val selectTab: (MainTab) -> Unit = { tab ->
        backStack.replaceAll(
            when (tab) {
                MainTab.Explore -> Home
                MainTab.Trips -> Trips
                MainTab.Favorites -> Favorites
                MainTab.Profile -> Profile
            },
        )
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.goBack() },
        entryProvider = entryProvider {
            // 01 Authentication
            entry<Login> {
                LoginScreen(
                    onLogin = { backStack.replaceAll(Home) },
                    onRegister = { backStack.add(Register) },
                    onForgotPassword = { backStack.add(ForgotPassword) },
                )
            }
            entry<Register> {
                RegisterScreen(
                    onCreateAccount = { backStack.add(OtpVerification(OtpPurpose.Register)) },
                    onLogin = { backStack.popUpTo(Login) },
                    // Legal pages are not built yet.
                    onTermsClick = {},
                    onPrivacyClick = {},
                )
            }
            entry<ForgotPassword> {
                ForgotPasswordScreen(
                    onBack = { backStack.goBack() },
                    onSendCode = { backStack.add(OtpVerification(OtpPurpose.ResetPassword)) },
                    onBackToLogin = { backStack.popUpTo(Login) },
                    // Support page is not built yet.
                    onSupport = {},
                )
            }
            entry<OtpVerification> { route ->
                OtpVerificationScreen(
                    destination = SampleData.USER_EMAIL,
                    onBack = { backStack.goBack() },
                    onVerified = {
                        when (route.purpose) {
                            OtpPurpose.Register -> backStack.replaceAll(Home)
                            OtpPurpose.ResetPassword -> backStack.add(NewPassword)
                        }
                    },
                    onChangeDestination = { backStack.goBack() },
                    onBackToLogin = { backStack.popUpTo(Login) },
                    onSupport = {},
                )
            }
            entry<NewPassword> {
                NewPasswordScreen(
                    email = SampleData.USER_EMAIL,
                    onBack = { backStack.goBack() },
                    onSave = { backStack.popUpTo(Login) },
                    onSupport = {},
                )
            }

            // Main tabs
            entry<Home> {
                HomeScreen(
                    onSearch = { backStack.add(Search) },
                    onSos = { backStack.add(Sos) },
                    onCamera = { backStack.add(CameraScan) },
                    onTrip = { backStack.add(DayItinerary) },
                    onSeeAllSuggestions = { backStack.add(SearchResults(SampleSearch.QUERY)) },
                    onPlace = { backStack.add(PlaceDetail(it.id)) },
                    onHistory = { backStack.add(SearchHistory) },
                    onTabSelect = selectTab,
                )
            }
            entry<Trips> {
                PlaceholderScreen(stringResource(R.string.tab_trips), selectedTab = MainTab.Trips, onTabSelect = selectTab)
            }
            entry<Favorites> {
                PlaceholderScreen(stringResource(R.string.tab_favorites), selectedTab = MainTab.Favorites, onTabSelect = selectTab)
            }
            entry<Profile> {
                PlaceholderScreen(stringResource(R.string.tab_profile), selectedTab = MainTab.Profile, onTabSelect = selectTab)
            }

            // 02 Explore & search
            entry<Search> {
                SearchScreen(
                    onBack = { backStack.goBack() },
                    onSearch = { query -> backStack.add(SearchResults(query.ifBlank { SampleSearch.QUERY })) },
                    onHistory = { backStack.add(SearchHistory) },
                )
            }
            entry<SearchResults> { route ->
                SearchResultsScreen(
                    query = route.query,
                    onEditQuery = { backStack.add(Search) },
                    onPlace = { backStack.add(PlaceDetail(it.id)) },
                )
            }
            entry<SearchHistory> {
                SearchHistoryScreen(
                    onBack = { backStack.goBack() },
                    onSearch = { query -> backStack.add(SearchResults(query)) },
                )
            }
            entry<PlaceDetail> { route ->
                PlaceDetailScreen(
                    place = SamplePlaces.byId(route.placeId),
                    onBack = { backStack.goBack() },
                    onProfile = { selectTab(MainTab.Profile) },
                    onReviews = { backStack.add(PlaceReviews(route.placeId)) },
                    onAddToVote = { backStack.add(AddToGroupVote(route.placeId)) },
                )
            }
            entry<PlaceReviews> { route ->
                PlaceReviewsScreen(
                    place = SamplePlaces.byId(route.placeId),
                    onBack = { backStack.goBack() },
                    // Writing a review is not designed yet.
                    onWriteReview = {},
                )
            }

            // Screens of later groups
            entry<Sos> { PlaceholderScreen(stringResource(R.string.sos_title), onBack = { backStack.goBack() }) }
            entry<CameraScan> { PlaceholderScreen(stringResource(R.string.camera_title), onBack = { backStack.goBack() }) }
            entry<DayItinerary> { PlaceholderScreen(stringResource(R.string.itinerary_title), onBack = { backStack.goBack() }) }
            entry<AddToGroupVote> {
                PlaceholderScreen(stringResource(R.string.detail_add_to_vote), onBack = { backStack.goBack() })
            }
        },
    )
}

/** Removes the top screen, but never the last one (the system then closes the app). */
private fun MutableList<NavKey>.goBack() {
    if (size > 1) removeAt(lastIndex)
}

/** Removes screens above [route] so it is on top again; keeps the root if [route] is not in the stack. */
private fun MutableList<NavKey>.popUpTo(route: NavKey) {
    while (size > 1 && last() != route) removeAt(lastIndex)
}

/** Starts a new history with only [route], e.g. after logging in so Back does not return to Login. */
private fun MutableList<NavKey>.replaceAll(route: NavKey) {
    add(route)
    while (size > 1) removeAt(0)
}
