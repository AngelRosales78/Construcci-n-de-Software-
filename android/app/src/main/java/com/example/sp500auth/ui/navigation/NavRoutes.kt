package com.example.sp500auth.ui.navigation

sealed class NavRoutes(val route: String) {
    object Login : NavRoutes("login")
    object Register : NavRoutes("register")
    object Dashboard : NavRoutes("dashboard")
    object ForgotPassword : NavRoutes("forgot_password")
    object Splash : NavRoutes("splash")
}
