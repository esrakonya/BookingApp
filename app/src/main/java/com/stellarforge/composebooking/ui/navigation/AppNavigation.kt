package com.stellarforge.composebooking.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stellarforge.composebooking.ui.screens.addeditservice.AddEditServiceScreen
import com.stellarforge.composebooking.ui.screens.auth.LoginScreen
import com.stellarforge.composebooking.ui.screens.auth.OwnerLoginScreen
import com.stellarforge.composebooking.ui.screens.auth.OwnerSignUpScreen
import com.stellarforge.composebooking.ui.screens.auth.SignUpScreen
import com.stellarforge.composebooking.ui.screens.servicelist.ServiceListScreen
import com.stellarforge.composebooking.ui.screens.confirmation.BookingConfirmationScreen
import com.stellarforge.composebooking.ui.screens.booking.BookingScreen
import com.stellarforge.composebooking.ui.screens.businessprofile.BusinessProfileScreen
import com.stellarforge.composebooking.ui.screens.manageservices.ManageServicesScreen
import com.stellarforge.composebooking.ui.screens.mybookings.MyBookingsScreen
import com.stellarforge.composebooking.ui.screens.customerprofile.CustomerProfileScreen
import com.stellarforge.composebooking.ui.screens.customerprofile.edit.EditCustomerProfileScreen
import com.stellarforge.composebooking.ui.screens.schedule.ScheduleScreen
import com.stellarforge.composebooking.ui.screens.splash.SplashScreen

/**
 * Main Navigation Graph for the application.
 * Handles routing between all screens (Customer, Owner, Auth, etc.).
 */
@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = ScreenRoutes.Splash.route
    ) {
        // --- Splash Screen ---
        composable(route = ScreenRoutes.Splash.route) {
            SplashScreen(
                navController = navController
            )
        }

        // --- Customer Authentication Screens ---
        composable(route = ScreenRoutes.Login.route) {
            LoginScreen(
                navController = navController
            )
        }

        composable(route = ScreenRoutes.SignUp.route) {
            SignUpScreen(
                navController = navController
            )
        }

        // --- Business Owner Authentication Screens ---
        composable(route = ScreenRoutes.OwnerLogin.route) {
            OwnerLoginScreen(
                navController = navController
            )
        }

        composable(route = ScreenRoutes.OwnerSignUp.route) {
            OwnerSignUpScreen(
                navController = navController
            )
        }

        // --- Customer Main Screens ---

        // Service List (Home Screen for Customers)
        composable(route = ScreenRoutes.ServiceList.route) {
            ServiceListScreen(
                navController = navController,
                onServiceClick = { serviceId ->
                    // Navigate to Booking Screen with the selected Service ID
                    navController.navigate(ScreenRoutes.BookingScreen.createRoute(serviceId))
                }
            )
        }

        // Booking Detail Screen
        composable(
            route = ScreenRoutes.BookingScreen.route, // Route: "booking_screen/{serviceId}"
            arguments = listOf(navArgument("serviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            BookingScreen(
                navController = navController,
                onBookingConfirmed = {
                    // Navigate to confirmation page and clear back stack to prevent going back to booking form
                    navController.navigate(ScreenRoutes.BookingConfirmation.route) {
                        popUpTo(backStackEntry.destination.id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Booking Success/Confirmation Screen
        composable(route = ScreenRoutes.BookingConfirmation.route) {
            BookingConfirmationScreen(
                onNavigateHome = {
                    // Return to Home (ServiceList) and clear history
                    navController.navigate(ScreenRoutes.ServiceList.route) {
                        popUpTo(ScreenRoutes.ServiceList.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // --- Business Owner Main Screens ---

        // Business Profile (Dashboard)
        composable(route = ScreenRoutes.BusinessProfile.route) {
            BusinessProfileScreen(navController = navController)
        }

        // Manage Services (List of services offered by the owner)
        composable(route = ScreenRoutes.ManageServices.route) {
            ManageServicesScreen(
                navController = navController,
                onAddService = {
                    // Navigate to Add/Edit screen with null ID (Create Mode)
                    navController.navigate(ScreenRoutes.AddEditService.createRoute(serviceId = null))
                },
                onEditService = { serviceId ->
                    // Navigate to Add/Edit screen with existing ID (Edit Mode)
                    navController.navigate(ScreenRoutes.AddEditService.createRoute(serviceId = serviceId))
                }
            )
        }

        // Add or Edit Service Screen
        composable(
            route = ScreenRoutes.AddEditService.route,
            arguments = listOf(
                navArgument("serviceId") {
                    type = NavType.StringType
                    nullable = true // Nullable allows using the same screen for 'Add' and 'Edit'
                }
            )
        ) {
            AddEditServiceScreen(navController = navController)
        }

        // --- Shared / Profile Screens ---

        // Customer Booking History
        composable(ScreenRoutes.MyBookings.route) {
            MyBookingsScreen(navController = navController)
        }

        // Owner Schedule View
        composable(ScreenRoutes.Schedule.route) {
            ScheduleScreen(navController = navController)
        }

        // Customer Profile & Settings
        composable(ScreenRoutes.CustomerProfile.route) {
            CustomerProfileScreen(navController = navController)
        }

        // Edit Customer Profile
        composable(route = ScreenRoutes.EditCustomerProfile.route) {
            EditCustomerProfileScreen(navController = navController)
        }
    }
}