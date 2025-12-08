package com.stellarforge.composebooking.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.stellarforge.composebooking.R

/**
 * Defines the items in the Bottom Navigation Bar.
 *
 * Each item holds a route (for navigation), a string resource ID (for localized titles),
 * and an icon.
 */
sealed class BottomNavItem(
    val route: String,
    @StringRes val titleResId: Int, // String yerine ID (Çoklu dil için)
    val icon: ImageVector
) {
    // --- CUSTOMER MENU ---
    object CustomerHome : BottomNavItem(
        route = ScreenRoutes.ServiceList.route,
        titleResId = R.string.service_list_title, // "Services"
        icon = Icons.Default.Home
    )
    object MyBookings : BottomNavItem(
        route = ScreenRoutes.MyBookings.route,
        titleResId = R.string.my_bookings_screen_title, // "My Bookings"
        icon = Icons.Default.DateRange
    )
    object CustomerProfile : BottomNavItem(
        route = ScreenRoutes.CustomerProfile.route,
        titleResId = R.string.screen_title_account, // "Account"
        icon = Icons.Default.Person
    )

    // --- OWNER MENU ---
    object Schedule : BottomNavItem(
        route = ScreenRoutes.Schedule.route,
        titleResId = R.string.schedule_screen_title, // "Schedule"
        icon = Icons.Default.CalendarToday
    )
    object ManageServices : BottomNavItem(
        route = ScreenRoutes.ManageServices.route,
        titleResId = R.string.manage_services_screen_title, // "Services" (Admin)
        icon = Icons.Default.List
    )
    object BusinessProfile : BottomNavItem(
        route = ScreenRoutes.BusinessProfile.route,
        titleResId = R.string.screen_title_business_profile, // "Profile"
        icon = Icons.Default.Settings
    )
}