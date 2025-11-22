package com.stellarforge.composebooking.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sports
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    // Customer
    object CustomerHome: BottomNavItem("Home", Icons.Default.Home, ScreenRoutes.ServiceList.route)
    object MyBookings: BottomNavItem("My Bookings", Icons.Default.Event, ScreenRoutes.MyBookings.route)

    //Business owner
    object Schedule: BottomNavItem("Program", Icons.Default.CalendarToday, ScreenRoutes.Schedule.route)
    object ManageServices: BottomNavItem("Services", Icons.Default.ContentCut, ScreenRoutes.ManageServices.route)
    object BusinessProfile: BottomNavItem("Profile", Icons.Default.Settings, ScreenRoutes.BusinessProfile.route)
    object CustomerProfile: BottomNavItem("Profile", Icons.Default.Person, ScreenRoutes.Profile.route)
}