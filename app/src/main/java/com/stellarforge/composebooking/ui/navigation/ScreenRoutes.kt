package com.stellarforge.composebooking.ui.navigation

// Uygulamadaki farklı ekranları ve rotalarını temsil eden sealed class
sealed class ScreenRoutes(val route: String) {
    object ServiceList : ScreenRoutes("service_list")
    object Login : ScreenRoutes("login")
    object SignUp : ScreenRoutes("signup")

    object BookingScreen : ScreenRoutes("booking_screen/{serviceId}") {
        fun createRoute(serviceId: String) = "booking_screen/$serviceId"
    }

    // Rezervasyon onay ekranı
    object BookingConfirmation : ScreenRoutes("booking_confirmation")

    object Splash : ScreenRoutes("splash")
}