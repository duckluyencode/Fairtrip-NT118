package com.tripfair.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.tripfair.app.data.AppContainer
import com.tripfair.app.data.session.SessionState
import com.tripfair.app.ui.components.MainTab
import com.tripfair.app.ui.sample.SampleCamera
import com.tripfair.app.ui.sample.SampleItinerary
import com.tripfair.app.ui.sample.SamplePlaces
import com.tripfair.app.ui.sample.SampleSearch
import com.tripfair.app.ui.screens.auth.ForgotPasswordRoute
import com.tripfair.app.ui.screens.auth.LoginRoute
import com.tripfair.app.ui.screens.auth.NewPasswordRoute
import com.tripfair.app.ui.screens.auth.OtpRoute
import com.tripfair.app.ui.screens.auth.RegisterRoute
import com.tripfair.app.ui.screens.camera.CameraScreen
import com.tripfair.app.ui.screens.camera.RecognitionResultScreen
import com.tripfair.app.ui.screens.explore.PlaceDetailRoute
import com.tripfair.app.ui.screens.explore.PlaceReviewsScreen
import com.tripfair.app.ui.screens.explore.SearchHistoryRoute
import com.tripfair.app.ui.screens.explore.SearchResultsRoute
import com.tripfair.app.ui.screens.explore.SearchRoute
import com.tripfair.app.ui.screens.group.CreateGroupRoute
import com.tripfair.app.ui.screens.group.CreateOrJoinGroupScreen
import com.tripfair.app.ui.screens.group.EditBudgetScreen
import com.tripfair.app.ui.screens.group.GroupPlanScreen
import com.tripfair.app.ui.screens.group.TripsRoute
import com.tripfair.app.ui.screens.group.TripsViewModel
import com.tripfair.app.ui.screens.group.InviteFriendsScreen
import com.tripfair.app.ui.screens.group.JoinGroupScreen
import com.tripfair.app.ui.screens.home.HomeScreen
import com.tripfair.app.ui.screens.home.HomeViewModel
import com.tripfair.app.ui.AppViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.ui.screens.legal.LegalDocumentScreen
import com.tripfair.app.ui.screens.legal.PrivacyPolicy
import com.tripfair.app.ui.screens.legal.TermsOfUse
import com.tripfair.app.ui.screens.profile.DeleteAccountRoute
import com.tripfair.app.ui.screens.profile.EditProfileRoute
import com.tripfair.app.ui.screens.profile.FavoritesRoute
import com.tripfair.app.ui.screens.profile.ProfileScreen
import com.tripfair.app.ui.screens.profile.SettingsScreen
import com.tripfair.app.ui.screens.profile.SupportScreen
import com.tripfair.app.ui.screens.sos.ContactFormRoute
import com.tripfair.app.ui.screens.sos.EmergencyContactsRoute
import com.tripfair.app.ui.screens.sos.SosScreen
import com.tripfair.app.ui.screens.sos.rememberLocationPermission
import com.tripfair.app.ui.screens.trip.CreateTripNext
import com.tripfair.app.ui.screens.trip.CreateTripRoute
import com.tripfair.app.ui.screens.trip.upcoming
import com.tripfair.app.data.trips.TripSummaryDto
import com.tripfair.app.ui.screens.trip.ItineraryEditorScreen
import com.tripfair.app.ui.screens.trip.ItineraryScreen
import com.tripfair.app.ui.screens.trip.LodgingScreen
import com.tripfair.app.ui.screens.trip.OptimizationResultScreen
import com.tripfair.app.ui.screens.trip.OptimizingScreen
import com.tripfair.app.ui.screens.vote.AddToVoteScreen
import com.tripfair.app.ui.screens.vote.BallotScreen
import com.tripfair.app.ui.screens.vote.VoteListScreen

/** Screens reachable without an account; the others need a signed-in session. */
private fun NavKey.isPublic(): Boolean =
    this == Login || this == Register || this == ForgotPassword || this is OtpVerification ||
        this == NewPassword || this == Support || this == PrivacyPolicyPage || this == TermsPage

/**
 * Waits for the saved session (a few milliseconds at app start) so the first screen is the
 * right one: Home when signed in, Login otherwise.
 */
@Composable
fun TripFairNavDisplay(container: AppContainer) {
    val session by container.session.state.collectAsState()
    if (session is SessionState.Restoring) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
    } else {
        TripFairNavHost(container, startSignedIn = session is SessionState.SignedIn)
    }
}

/**
 * Connects every button to its target screen, following the flow notes in Figma.
 * The back stack is a plain list: navigating adds a route, going back removes the last one.
 */
@Composable
private fun TripFairNavHost(container: AppContainer, startSignedIn: Boolean) {
    val backStack = rememberNavBackStack(if (startSignedIn) Home else Login)
    val session by container.session.state.collectAsState()
    val account by container.authRepository.currentAccount.collectAsState()

    // The session can end anywhere (sign out, account deleted, refresh token refused):
    // then only the login screens stay reachable.
    LaunchedEffect(session) {
        if (session is SessionState.SignedOut && backStack.any { !it.isPublic() }) {
            backStack.replaceAll(Login)
        }
    }

    // Signing out ends the session, then starts a new history at the login screen.
    val logout: () -> Unit = {
        container.authRepository.logout()
        backStack.replaceAll(Login)
    }

    // The itinerary is shared by several screens; it stays here until trips come from the backend.
    var itinerary by remember { mutableStateOf(SampleItinerary.days) }
    val sosContacts by container.emergencyContactsRepository.contacts.collectAsState()

    // Shows the itinerary, going back to it if it is already open below.
    val showItinerary: (tripId: Int) -> Unit = { tripId ->
        val route = DayItinerary(tripId)
        if (route in backStack) backStack.popUpTo(route) else backStack.add(route)
    }

    // A group opens on its members and votes; a solo trip on its itinerary, planned first if needed.
    val openTrip: (TripSummaryDto) -> Unit = { trip ->
        backStack.add(
            when {
                trip.isGroup -> GroupPlan(trip.id)
                trip.isPlanned -> DayItinerary(trip.id)
                else -> Optimizing(trip.id, group = false)
            },
        )
    }
    val scheduleTrip: (TripSummaryDto) -> Unit = { trip ->
        if (trip.isPlanned) backStack.add(DayItinerary(trip.id)) else openTrip(trip)
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
        // Each screen keeps its saved UI state and its own ViewModels, both cleared when it is removed.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            // 01 Authentication
            entry<Login> {
                LoginRoute(
                    onLoggedIn = { backStack.replaceAll(Home) },
                    onRegister = { backStack.add(Register) },
                    onForgotPassword = { backStack.add(ForgotPassword) },
                )
            }
            entry<Register> {
                RegisterRoute(
                    onCodeSent = { backStack.add(OtpVerification(OtpPurpose.Register)) },
                    onLogin = { backStack.popUpTo(Login) },
                    onTermsClick = { backStack.add(TermsPage) },
                    onPrivacyClick = { backStack.add(PrivacyPolicyPage) },
                )
            }
            entry<ForgotPassword> {
                ForgotPasswordRoute(
                    onBack = { backStack.goBack() },
                    onCodeSent = { backStack.add(OtpVerification(OtpPurpose.ResetPassword)) },
                    onBackToLogin = { backStack.popUpTo(Login) },
                    onSupport = { backStack.add(Support) },
                )
            }
            entry<OtpVerification> { route ->
                OtpRoute(
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
                NewPasswordRoute(
                    onBack = { backStack.goBack() },
                    onSaved = { backStack.popUpTo(Login) },
                    onSupport = { backStack.add(Support) },
                )
            }

            // Main tabs
            entry<Home> {
                val home: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
                HomeScreen(
                    userName = account?.fullName.orEmpty(),
                    suggestions = home.suggestions,
                    upcomingTrip = home.upcomingTrip,
                    onSearch = { backStack.add(Search) },
                    onSos = { backStack.add(Sos) },
                    onCamera = { backStack.add(CameraScan) },
                    onTrip = openTrip,
                    onSeeAllSuggestions = { backStack.add(SearchResults(SampleSearch.QUERY)) },
                    onPlace = { backStack.add(PlaceDetail(it.toString())) },
                    onHistory = { backStack.add(SearchHistory) },
                    onTabSelect = selectTab,
                )
            }
            entry<Trips> {
                TripsRoute(
                    onCreateTrip = { backStack.add(CreateTrip) },
                    onJoinGroup = { backStack.add(JoinGroup) },
                    onOpenTrip = openTrip,
                    onSchedule = scheduleTrip,
                    onInvite = { backStack.add(InviteFriends(it)) },
                    onVote = { backStack.add(Ballot(it)) },
                    onPlaces = { backStack.add(SearchResults(SampleSearch.QUERY)) },
                    onTabSelect = selectTab,
                )
            }
            entry<Favorites> {
                FavoritesRoute(
                    onPlace = { backStack.add(PlaceDetail(it.toString())) },
                    onExplore = { selectTab(MainTab.Explore) },
                    onTabSelect = selectTab,
                )
            }
            entry<Profile> {
                val trips: TripsViewModel = viewModel(factory = AppViewModelProvider.Factory)
                LaunchedEffect(Unit) { trips.load() }
                ProfileScreen(
                    account = account,
                    emergencyContacts = sosContacts,
                    latestTrip = trips.items.upcoming(),
                    onEditProfile = { backStack.add(EditProfile) },
                    onTrip = { selectTab(MainTab.Trips) },
                    onFavorites = { selectTab(MainTab.Favorites) },
                    onEmergencyContacts = { backStack.add(EmergencyContacts) },
                    onSettings = { backStack.add(Settings) },
                    onHelp = { backStack.add(Support) },
                    onLogout = logout,
                    onTabSelect = selectTab,
                )
            }

            // 02 Explore & search
            entry<Search> {
                SearchRoute(
                    onBack = { backStack.goBack() },
                    onSearch = { query -> backStack.add(SearchResults(query)) },
                    onPlace = { id, sessionToken -> backStack.add(PlaceDetail(id.toString(), sessionToken)) },
                    onHistory = { backStack.add(SearchHistory) },
                )
            }
            entry<SearchResults> { route ->
                SearchResultsRoute(
                    query = route.query,
                    onEditQuery = { backStack.add(Search) },
                    onPlace = { backStack.add(PlaceDetail(it.toString())) },
                )
            }
            entry<SearchHistory> {
                SearchHistoryRoute(
                    onBack = { backStack.goBack() },
                    onSearch = { query -> backStack.add(SearchResults(query)) },
                )
            }
            entry<PlaceDetail> { route ->
                PlaceDetailRoute(
                    placeId = route.placeId,
                    sessionToken = route.sessionToken,
                    onBack = { backStack.goBack() },
                    onProfile = { selectTab(MainTab.Profile) },
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
                EditProfileRoute(onBack = { backStack.goBack() }, onSaved = { backStack.goBack() })
            }
            entry<DeleteAccount> {
                DeleteAccountRoute(onBack = { backStack.goBack() }, onDeleted = { backStack.replaceAll(Login) })
            }
            entry<Settings> {
                SettingsScreen(
                    account = account,
                    onBack = { backStack.goBack() },
                    onEditProfile = { backStack.add(EditProfile) },
                    onPrivacy = { backStack.add(PrivacyPolicyPage) },
                    onSupport = { backStack.add(Support) },
                    onTerms = { backStack.add(TermsPage) },
                    onDeleteAccount = { backStack.add(DeleteAccount) },
                    onLogout = logout,
                )
            }
            entry<Support> { SupportScreen(onBack = { backStack.goBack() }) }
            entry<PrivacyPolicyPage> { LegalDocumentScreen(document = PrivacyPolicy, onBack = { backStack.goBack() }) }
            entry<TermsPage> { LegalDocumentScreen(document = TermsOfUse, onBack = { backStack.goBack() }) }

            // 03 Groups & members
            entry<GroupChoice> {
                CreateOrJoinGroupScreen(
                    onBack = { backStack.goBack() },
                    onCreate = { backStack.add(CreateTrip) },
                    onJoin = { backStack.add(JoinGroup) },
                )
            }
            entry<CreateGroup> { route ->
                CreateGroupRoute(
                    draft = route.draft,
                    ownerName = account?.fullName.orEmpty(),
                    onBack = { backStack.goBack() },
                    onCreated = { created ->
                        // The group exists now: Back from it skips the two forms that made it.
                        backStack.removeAll { it is CreateTrip || it is CreateGroup || it is GroupChoice }
                        backStack.add(GroupPlan(created.tripId))
                        if (created.invite) backStack.add(InviteFriends(created.tripId))
                    },
                )
            }
            entry<JoinGroup> {
                JoinGroupScreen(onBack = { backStack.goBack() }, onJoin = { selectTab(MainTab.Trips) })
            }
            entry<InviteFriends> { InviteFriendsScreen(onBack = { backStack.goBack() }) }
            entry<GroupPlan> { route ->
                GroupPlanScreen(
                    onBack = { backStack.goBack() },
                    onInvite = { backStack.add(InviteFriends(route.tripId)) },
                    onEditBudget = { backStack.add(EditBudget(route.tripId)) },
                    onStartVote = { backStack.add(VoteList(route.tripId)) },
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
                    onAdd = { backStack.goBack() },
                )
            }
            entry<VoteList> { route ->
                VoteListScreen(onBack = { backStack.goBack() }, onFinish = { backStack.add(LodgingChoice(route.tripId, group = true)) })
            }
            entry<Ballot> {
                BallotScreen(onBack = { backStack.goBack() }, onAddPlace = { backStack.add(Search) })
            }

            // 05 Trip & optimization, 10 Solo trip & lodging
            entry<CreateTrip> {
                CreateTripRoute(
                    onBack = { backStack.goBack() },
                    onProfile = { selectTab(MainTab.Profile) },
                    onNext = { next ->
                        when (next) {
                            is CreateTripNext.NameGroup -> backStack.add(CreateGroup(next.draft))
                            // The solo trip exists now, so the form is not kept below the next screen.
                            is CreateTripNext.ChooseLodging -> backStack.replaceTop(LodgingChoice(next.tripId, group = false))
                            is CreateTripNext.Plan -> backStack.replaceTop(Optimizing(next.tripId, group = false))
                        }
                    },
                )
            }
            entry<LodgingChoice> { route ->
                LodgingScreen(onBack = { backStack.goBack() }, onConfirm = { backStack.add(Optimizing(route.tripId, route.group)) })
            }
            entry<Optimizing> { route ->
                OptimizingScreen(
                    onBack = { backStack.goBack() },
                    onDone = {
                        itinerary = SampleItinerary.days
                        // The progress screen is replaced, so Back does not return to it.
                        backStack.replaceTop(if (route.group) OptimizationResult(route.tripId) else DayItinerary(route.tripId))
                    },
                )
            }
            entry<OptimizationResult> { route ->
                OptimizationResultScreen(
                    onBack = { backStack.goBack() },
                    onProfile = { selectTab(MainTab.Profile) },
                    onViewItinerary = { showItinerary(route.tripId) },
                )
            }
            entry<DayItinerary> { route ->
                ItineraryScreen(
                    days = itinerary,
                    onBack = { backStack.goBack() },
                    onEdit = { backStack.add(ItineraryEditor(route.tripId)) },
                    onPlace = { backStack.add(PlaceDetail(it)) },
                )
            }
            entry<ItineraryEditor> { route ->
                ItineraryEditorScreen(
                    initialDays = SampleItinerary.editDraft(itinerary),
                    onBack = { backStack.goBack() },
                    onSave = { days ->
                        itinerary = days
                        backStack.goBack()
                    },
                    onAddPlace = { backStack.add(Search) },
                    onReoptimize = { backStack.replaceTop(Optimizing(route.tripId, group = true)) },
                )
            }

            // 06 Camera & SOS
            entry<CameraScan> {
                CameraScreen(
                    onBack = { backStack.goBack() },
                    onHelp = { backStack.add(Support) },
                    onResult = { recognized -> backStack.add(RecognitionResult(recognized)) },
                )
            }
            entry<RecognitionResult> { route ->
                RecognitionResultScreen(
                    result = if (route.recognized) SampleCamera.recognized else SampleCamera.notRecognized,
                    onBack = { backStack.goBack() },
                    onRetake = { backStack.popUpTo(CameraScan) },
                    onPlace = { backStack.add(PlaceDetail(it)) },
                    onViewTrip = { selectTab(MainTab.Trips) },
                    onSearchByName = { backStack.add(Search) },
                )
            }
            entry<Sos> {
                val location = rememberLocationPermission()
                SosScreen(
                    contacts = sosContacts,
                    hasLocationPermission = location.granted,
                    onRequestLocation = location.request,
                    onBack = { backStack.goBack() },
                    onContacts = { backStack.add(EmergencyContacts) },
                )
            }
            entry<EmergencyContacts> {
                EmergencyContactsRoute(
                    onBack = { backStack.goBack() },
                    onSaved = { backStack.goBack() },
                    onAdd = { backStack.add(ContactForm()) },
                    onEdit = { backStack.add(ContactForm(it)) },
                )
            }
            entry<ContactForm> { route ->
                ContactFormRoute(
                    contactId = route.contactId,
                    onBack = { backStack.goBack() },
                    onSaved = { backStack.goBack() },
                )
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
