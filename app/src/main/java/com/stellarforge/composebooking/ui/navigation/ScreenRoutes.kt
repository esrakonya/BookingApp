package com.stellarforge.composebooking.ui.navigation

/**
 * Constants for Navigation Arguments.
 * These keys are used to retrieve data from [SavedStateHandle] in ViewModels.
 */
object RouteArgs {
    const val SERVICE_ID = "serviceId"
}

/**
 * A centralized sealed class defining all navigation routes in the application.
 *
 * **Structure:**
 * - Simple routes are defined as objects (e.g., [Login]).
 * - Routes with arguments are defined as objects with helper functions to construct the path (e.g., [BookingScreen]).
 */
sealed class ScreenRoutes(val route: String) {

    // --- INITIALIZATION ---
    object Splash : ScreenRoutes("splash")

    // --- AUTHENTICATION ---
    object Login : ScreenRoutes("login") // Customer Login
    object SignUp : ScreenRoutes("signup") // Customer Sign Up
    object OwnerLogin : ScreenRoutes("owner_login_screen")
    object OwnerSignUp : ScreenRoutes("owner_signup_screen")

    // --- CUSTOMER FLOW ---
    object ServiceList : ScreenRoutes("service_list") // Home / Storefront
    object MyBookings : ScreenRoutes("my_bookings")

    // Renamed from 'Profile' to 'CustomerProfile' for clarity vs 'BusinessProfile'
    object CustomerProfile : ScreenRoutes("customer_profile_screen")

    object EditCustomerProfile : ScreenRoutes("edit_customer_profile")

    // Route with MANDATORY argument (Path Parameter)
    object BookingScreen : ScreenRoutes("booking_screen/{${RouteArgs.SERVICE_ID}}") {
        fun createRoute(serviceId: String) = "booking_screen/$serviceId"
    }

    object BookingConfirmation : ScreenRoutes("booking_confirmation")

    // --- OWNER FLOW ---
    object Schedule : ScreenRoutes("schedule") // Owner Dashboard
    object BusinessProfile : ScreenRoutes("business_profile_screen")
    object ManageServices : ScreenRoutes("manage_services")

    // Route with OPTIONAL argument (Query Parameter)
    // Used for both Adding (no ID) and Editing (with ID) a service.
    object AddEditService : ScreenRoutes("add_edit_service?${RouteArgs.SERVICE_ID}={${RouteArgs.SERVICE_ID}}") {
        /**
         * Helper to build the navigation route.
         * @param serviceId If null, opens in "Add Mode". If provided, opens in "Edit Mode".
         */
        fun createRoute(serviceId: String? = null): String {
            return if (serviceId != null) {
                "add_edit_service?${RouteArgs.SERVICE_ID}=$serviceId"
            } else {
                "add_edit_service"
            }
        }
    }
}