package com.rakshax.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Device : Screen("device")
    object SafetyMap : Screen("safety_map")
    object Contacts : Screen("contacts")
    object SosStatus : Screen("sos_status")
    object Profile : Screen("profile")
}
