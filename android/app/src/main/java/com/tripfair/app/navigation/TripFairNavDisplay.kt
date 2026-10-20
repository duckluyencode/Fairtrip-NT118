package com.tripfair.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.tripfair.app.ui.sample.SampleData
import com.tripfair.app.ui.screens.auth.ForgotPasswordScreen
import com.tripfair.app.ui.screens.auth.LoginScreen
import com.tripfair.app.ui.screens.auth.NewPasswordScreen
import com.tripfair.app.ui.screens.auth.OtpVerificationScreen
import com.tripfair.app.ui.screens.auth.RegisterScreen
import com.tripfair.app.ui.screens.home.HomeScreen

/**
 * Connects every button to its target screen, following the flow notes in Figma.
 * The back stack is a plain list: navigating adds a route, going back removes the last one.
 */
@Composable
fun TripFairNavDisplay() {
    val backStack = rememberNavBackStack(Login)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.goBack() },
        entryProvider = entryProvider {
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
            entry<Home> {
                HomeScreen()
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
