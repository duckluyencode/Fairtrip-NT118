package com.tripfair.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.tripfair.app.R
import com.tripfair.app.ui.components.MainTab
import com.tripfair.app.ui.sample.SampleData
import com.tripfair.app.ui.sample.SampleItinerary
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
import com.tripfair.app.ui.screens.group.CreateGroupScreen
import com.tripfair.app.ui.screens.group.CreateOrJoinGroupScreen
import com.tripfair.app.ui.screens.group.EditBudgetScreen
import com.tripfair.app.ui.screens.group.GroupPlanScreen
import com.tripfair.app.ui.screens.group.GroupsScreen
import com.tripfair.app.ui.screens.group.InviteFriendsScreen
import com.tripfair.app.ui.screens.group.JoinGroupScreen
import com.tripfair.app.ui.screens.home.HomeScreen
import com.tripfair.app.ui.screens.legal.LegalDocumentScreen
import com.tripfair.app.ui.screens.legal.PrivacyPolicy
import com.tripfair.app.ui.screens.legal.TermsOfUse
import com.tripfair.app.ui.screens.profile.EditProfileScreen
import com.tripfair.app.ui.screens.profile.FavoritesScreen
import com.tripfair.app.ui.screens.profile.ProfileScreen
import com.tripfair.app.ui.screens.profile.SettingsScreen
import com.tripfair.app.ui.screens.profile.SupportScreen
import com.tripfair.app.ui.screens.trip.CreateTripScreen
import com.tripfair.app.ui.screens.trip.ItineraryEditorScreen
import com.tripfair.app.ui.screens.trip.ItineraryScreen
import com.tripfair.app.ui.screens.trip.LodgingScreen
import com.tripfair.app.ui.screens.trip.OptimizationResultScreen
import com.tripfair.app.ui.screens.trip.OptimizingScreen
import com.tripfair.app.ui.screens.vote.AddToVoteScreen
import com.tripfair.app.ui.screens.vote.BallotScreen
import com.tripfair.app.ui.screens.vote.VoteListScreen

/**
 * Connects every button to its target screen, following the flow notes in Figma.
 * The back stack is a plain list: navigating adds a route, going back removes the last one.
 */
@Composable
fun TripFairNavDisplay() {
    val backStack = rememberNavBackStack(Login)

    // The saved itinerary is shared by the itinerary screen and its editor until there is a backend.
    var itinerary by remember { mutableStateOf(SampleItinerary.days) }

    // Shows the itinerary, going back to it if it is already open below.
    val showItinerary: () -> Unit = {
        if (DayItinerary in backStack) backStack.popUpTo(DayItinerary) else backStack.add(DayItinerary)
    }

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
                    onTermsClick = { backStack.add(TermsPage) },
                    onPrivacyClick = { backStack.add(PrivacyPolicyPage) },
                )
            }
            entry<ForgotPassword> {
                ForgotPasswordScreen(
                    onBack = { backStack.goBack() },
                    onSendCode = { backStack.add(OtpVerification(OtpPurpose.ResetPassword)) },
                    onBackToLogin = { backStack.popUpTo(Login) },
                    onSupport = { backStack.add(Support) },
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
                    onSupport = { backStack.add(Support) },
                )
            }
            entry<NewPassword> {
                NewPasswordScreen(
                    email = SampleData.USER_EMAIL,
                    onBack = { backStack.goBack() },
                    onSave = { backStack.popUpTo(Login) },
                    onSupport = { backStack.add(Support) },
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
                GroupsScreen(
                    onNewGroup = { backStack.add(GroupChoice) },
                    onOpenGroup = { backStack.add(GroupPlan) },
                    onInvite = { backStack.add(InviteFriends) },
                    onSchedule = { backStack.add(DayItinerary) },
                    onVote = { backStack.add(Ballot) },
                    onPlaces = { backStack.add(SearchResults(SampleSearch.QUERY)) },
                    onCreateTrip = { backStack.add(CreateTrip) },
                    onJoinGroup = { backStack.add(JoinGroup) },
                    onTabSelect = selectTab,
                )
            }
            entry<Favorites> {
                FavoritesScreen(
                    onPlace = { backStack.add(PlaceDetail(it.id)) },
                    onExplore = { selectTab(MainTab.Explore) },
                    onTabSelect = selectTab,
                )
            }
            entry<Profile> {
                ProfileScreen(
                    onEditProfile = { backStack.add(EditProfile) },
                    onTrip = { backStack.add(DayItinerary) },
                    onFavorites = { selectTab(MainTab.Favorites) },
                    onEmergencyContacts = { backStack.add(EmergencyContacts) },
                    onSettings = { backStack.add(Settings) },
                    onHelp = { backStack.add(Support) },
                    onLogout = { backStack.replaceAll(Login) },
                    onTabSelect = selectTab,
                )
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

            // 08 Profile & favorites
            entry<EditProfile> {
                EditProfileScreen(onBack = { backStack.goBack() }, onSave = { backStack.goBack() })
            }
            entry<Settings> {
                SettingsScreen(
                    onBack = { backStack.goBack() },
                    onEditProfile = { backStack.add(EditProfile) },
                    onPrivacy = { backStack.add(PrivacyPolicyPage) },
                    onSupport = { backStack.add(Support) },
                    onTerms = { backStack.add(TermsPage) },
                    onLogout = { backStack.replaceAll(Login) },
                )
            }
            entry<Support> { SupportScreen(onBack = { backStack.goBack() }) }
            entry<PrivacyPolicyPage> { LegalDocumentScreen(document = PrivacyPolicy, onBack = { backStack.goBack() }) }
            entry<TermsPage> { LegalDocumentScreen(document = TermsOfUse, onBack = { backStack.goBack() }) }

            // 03 Groups & members
            entry<GroupChoice> {
                CreateOrJoinGroupScreen(
                    onBack = { backStack.goBack() },
                    onCreate = { backStack.add(CreateGroup) },
                    onJoin = { backStack.add(JoinGroup) },
                )
            }
            entry<CreateGroup> {
                CreateGroupScreen(
                    onBack = { backStack.goBack() },
                    onInvite = { backStack.add(InviteFriends) },
                    onContinue = { backStack.add(GroupPlan) },
                )
            }
            entry<JoinGroup> {
                JoinGroupScreen(onBack = { backStack.goBack() }, onJoin = { selectTab(MainTab.Trips) })
            }
            entry<InviteFriends> { InviteFriendsScreen(onBack = { backStack.goBack() }) }
            entry<GroupPlan> {
                GroupPlanScreen(
                    onBack = { backStack.goBack() },
                    onInvite = { backStack.add(InviteFriends) },
                    onEditBudget = { backStack.add(EditBudget) },
                    onStartVote = { backStack.add(VoteList) },
                )
            }
            entry<EditBudget> {
                EditBudgetScreen(onBack = { backStack.goBack() }, onSave = { backStack.goBack() })
            }

            // 04 Votes
            entry<AddToGroupVote> { route ->
                AddToVoteScreen(
                    place = SamplePlaces.byId(route.placeId),
                    onBack = { backStack.goBack() },
                    onBackToDetail = { backStack.goBack() },
                    onAdd = { backStack.replaceTop(VoteList) },
                )
            }
            entry<VoteList> {
                VoteListScreen(onBack = { backStack.goBack() }, onFinish = { backStack.add(LodgingChoice(group = true)) })
            }
            entry<Ballot> {
                BallotScreen(onBack = { backStack.goBack() }, onAddPlace = { backStack.add(Search) })
            }

            // 05 Trip & optimization, 10 Solo trip & lodging
            entry<CreateTrip> {
                CreateTripScreen(
                    onBack = { backStack.goBack() },
                    onProfile = { selectTab(MainTab.Profile) },
                    onContinueGroup = { backStack.add(CreateGroup) },
                    onChooseLodging = { backStack.add(LodgingChoice(group = false)) },
                    onPlanWithoutLodging = { backStack.add(Optimizing(group = false)) },
                )
            }
            entry<LodgingChoice> { route ->
                LodgingScreen(onBack = { backStack.goBack() }, onConfirm = { backStack.add(Optimizing(route.group)) })
            }
            entry<Optimizing> { route ->
                OptimizingScreen(
                    onBack = { backStack.goBack() },
                    onDone = {
                        itinerary = SampleItinerary.days
                        // The progress screen is replaced, so Back does not return to it.
                        backStack.replaceTop(if (route.group) OptimizationResult else DayItinerary)
                    },
                )
            }
            entry<OptimizationResult> {
                OptimizationResultScreen(
                    onBack = { backStack.goBack() },
                    onProfile = { selectTab(MainTab.Profile) },
                    onViewItinerary = showItinerary,
                )
            }
            entry<DayItinerary> {
                ItineraryScreen(
                    days = itinerary,
                    onBack = { backStack.goBack() },
                    onEdit = { backStack.add(ItineraryEditor) },
                    onPlace = { backStack.add(PlaceDetail(it)) },
                )
            }
            entry<ItineraryEditor> {
                ItineraryEditorScreen(
                    initialDays = SampleItinerary.editDraft(itinerary),
                    onBack = { backStack.goBack() },
                    onSave = { days ->
                        itinerary = days
                        backStack.goBack()
                    },
                    onAddPlace = { backStack.add(Search) },
                    onReoptimize = { backStack.replaceTop(Optimizing(group = true)) },
                )
            }

            // Screens of later groups
            entry<EmergencyContacts> {
                PlaceholderScreen(stringResource(R.string.emergency_contacts_title), onBack = { backStack.goBack() })
            }
            entry<Sos> { PlaceholderScreen(stringResource(R.string.sos_title), onBack = { backStack.goBack() }) }
            entry<CameraScan> { PlaceholderScreen(stringResource(R.string.camera_title), onBack = { backStack.goBack() }) }
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

/** Replaces the top screen with [route], e.g. a finished progress screen with its result. */
private fun MutableList<NavKey>.replaceTop(route: NavKey) {
    add(route)
    if (size > 1) removeAt(lastIndex - 1)
}

/** Starts a new history with only [route], e.g. after logging in so Back does not return to Login. */
private fun MutableList<NavKey>.replaceAll(route: NavKey) {
    add(route)
    while (size > 1) removeAt(0)
}
