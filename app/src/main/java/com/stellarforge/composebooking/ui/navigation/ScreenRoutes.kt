package com.stellarforge.composebooking.ui.navigation

import okhttp3.Route
object RouteArgs {
    const val SERVICE_ID = "serviceId"
}
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

    object BusinessProfile : ScreenRoutes("business_profile_screen")

    object ManageServices: ScreenRoutes("manage_services")

    object AddEditService: ScreenRoutes("add_edit_service?${RouteArgs.SERVICE_ID}={${RouteArgs.SERVICE_ID}}") {
        // Bu fonksiyon, navigasyon için tam ve doğru URL'yi oluşturur.
        // `navController.navigate(AddEditService.createRoute(serviceId = "123"))`
        fun createRoute(serviceId: String?): String {
            return if (serviceId != null) {
                // Düzenleme modu: serviceId ile git
                "add_edit_service?${RouteArgs.SERVICE_ID}=$serviceId"
            } else {
                // Ekleme modu: serviceId olmadan git (query parametresi olmadan)
                "add_edit_service"
            }
        }
    }

    object MyBookings: ScreenRoutes("my_bookings")

    object Schedule: ScreenRoutes("schedule")

    object Profile: ScreenRoutes("profile_screen")

    object OwnerLogin: ScreenRoutes("owner_login_screen")

    object OwnerSignUp: ScreenRoutes("owner_signup_screen")
}