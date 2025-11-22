package com.stellarforge.composebooking.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.stellarforge.composebooking.ui.screens.profile.ProfileEvent
import com.stellarforge.composebooking.ui.screens.profile.ProfileScreen
import com.stellarforge.composebooking.ui.screens.profile.ProfileViewModel
import com.stellarforge.composebooking.ui.screens.schedule.ScheduleScreen
import com.stellarforge.composebooking.ui.screens.splash.SplashScreen
import com.stellarforge.composebooking.ui.screens.splash.SplashViewModel
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = ScreenRoutes.Splash.route
    ) {
        composable(route = ScreenRoutes.Splash.route) {
            val viewModel: SplashViewModel = hiltViewModel()
            val startDestination by viewModel.startDestination.collectAsState()

            LaunchedEffect(startDestination) {
                Timber.d("Splash LaunchedEffect triggered with startDestination: $startDestination")
                startDestination?.let { destination ->
                    navController.navigate(destination) {
                        popUpTo(ScreenRoutes.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            SplashScreen()
        }

        // Müşteri Auth Ekranları
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

        // İşletme Sahibi Ekranları
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

        // Müşteri Ana Ekranları
        composable(route = ScreenRoutes.ServiceList.route) {
            // Bu rota çağrıldığında ServiceListScreen Composable'ını göster
            ServiceListScreen(
                // ViewModel zaten Hilt tarafından sağlanacak (Composable içinde)
                navController = navController,
                onServiceClick = { serviceId ->
                    // Rezervasyon ekranına git
                    navController.navigate(ScreenRoutes.BookingScreen.createRoute(serviceId))
                }
            )
        }

        composable(
            route = ScreenRoutes.BookingScreen.route, // "booking_screen/{serviceId}"
            arguments = listOf(navArgument("serviceId") { type = NavType.StringType }) // Argümanı tanımla
        ) { backStackEntry ->
            BookingScreen(
                navController = navController,
                onBookingConfirmed = {
                    navController.navigate(ScreenRoutes.BookingConfirmation.route) {
                        popUpTo(backStackEntry.destination.id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = ScreenRoutes.BookingConfirmation.route) {
            // BookingConfirmationScreen Composable'ını göster
            BookingConfirmationScreen(
                onNavigateHome = {
                    // Ana ekrana (ServiceList) dönmek için
                    // Zaten popUpTo ile ServiceList'e dönmüştük,
                    // ama kullanıcı tekrar dönebilmeli.
                    navController.navigate(ScreenRoutes.ServiceList.route) {
                        popUpTo(ScreenRoutes.ServiceList.route) { inclusive = true } // ServiceList dahil hepsini temizle
                        launchSingleTop = true
                    }
                }
            )
        }

        // İşletme Sahibi Ana Ekranları
        composable(route = ScreenRoutes.BusinessProfile.route) {
            BusinessProfileScreen(navController = navController)
        }

        composable(route = ScreenRoutes.ManageServices.route) {
            ManageServicesScreen(
                navController = navController,
                onAddService = {
                    navController.navigate(ScreenRoutes.AddEditService.createRoute(serviceId = null))
                },
                onEditService = { serviceId ->
                    navController.navigate(ScreenRoutes.AddEditService.createRoute(serviceId = serviceId))
                }
            )
        }

        composable(
            route = ScreenRoutes.AddEditService.route,
            arguments = listOf(
                navArgument("serviceId") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) {
            AddEditServiceScreen(navController = navController)
        }

        composable(ScreenRoutes.MyBookings.route) {
            MyBookingsScreen(navController = navController)
        }

        composable(ScreenRoutes.Schedule.route) {
            ScheduleScreen(navController = navController)
        }

        composable(ScreenRoutes.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()

            LaunchedEffect(key1 = true) {
                viewModel.eventFlow.collectLatest { event ->
                    when(event) {
                        ProfileEvent.NavigateToLogin -> {
                            navController.navigate(ScreenRoutes.Login.route) {
                                popUpTo(0)
                            }
                        }
                    }
                }
            }
            ProfileScreen(navController = navController, viewModel = viewModel)
        }


    }
}