package com.stellarforge.composebooking.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stellarforge.composebooking.ui.screens.auth.LoginScreen
import com.stellarforge.composebooking.ui.screens.auth.SignUpScreen
import com.stellarforge.composebooking.ui.screens.servicelist.ServiceListScreen
import com.stellarforge.composebooking.ui.screens.confirmation.BookingConfirmationScreen
import com.stellarforge.composebooking.ui.screens.booking.BookingScreen
import com.stellarforge.composebooking.ui.screens.splash.SplashScreen
import com.stellarforge.composebooking.ui.screens.splash.SplashViewModel
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

        // Auth Ekranları
        composable(route = ScreenRoutes.Login.route) {
            LoginScreen(
                // ViewModel zaten Hilt tarafından sağlanacak (Composable içinde)
                onLoginSuccess = {
                    // Başarılı giriş sonrası ServiceList'e git ve Login'i stack'ten kaldır
                    navController.navigate(ScreenRoutes.ServiceList.route) {
                        popUpTo(ScreenRoutes.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToSignUp = {
                    // Kayıt ekranına git
                    navController.navigate(ScreenRoutes.SignUp.route)
                }
            )
        }

        composable(route = ScreenRoutes.SignUp.route) {
            SignUpScreen(
                // ViewModel zaten Hilt tarafından sağlanacak (Composable içinde)
                onSignUpSuccess = {
                    // Başarılı kayıt sonrası ServiceList'e git ve Auth ekranlarını temizle
                    navController.navigate(ScreenRoutes.ServiceList.route) {
                        // Login'e kadar (Login dahil) temizle
                        popUpTo(ScreenRoutes.Login.route) { inclusive = true }
                        launchSingleTop = true
                        // Alternatif: Grafiğin başlangıcına kadar temizle
                        // popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    // Bir önceki ekrana (Login'e) dön
                    navController.popBackStack()
                }
            )
        }

        // ServiceList Ekranı
        composable(route = ScreenRoutes.ServiceList.route) {
            // Bu rota çağrıldığında ServiceListScreen Composable'ını göster
            ServiceListScreen(
                // ViewModel zaten Hilt tarafından sağlanacak (Composable içinde)
                onServiceClick = { serviceId ->
                    // Rezervasyon ekranına git
                    navController.navigate(ScreenRoutes.BookingScreen.createRoute(serviceId))
                },
                onNavigateToLogin = {
                    // Oturum kapatıldığında Login'e git ve tüm geri yığınını temizle
                    navController.navigate(ScreenRoutes.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true // Splash dahil tüm stack'i temizle
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        // BookingScreen Ekranı (Argümanlı)
        composable(
            route = ScreenRoutes.BookingScreen.route, // "booking_screen/{serviceId}"
            arguments = listOf(navArgument("serviceId") { type = NavType.StringType }) // Argümanı tanımla
        ) { backStackEntry ->
            // Argümanı al
            val serviceId = backStackEntry.arguments?.getString("serviceId")
            if (serviceId != null) {
                // BookingScreen Composable'ını göster ve serviceId'yi ilet
                BookingScreen(
                    navController = navController, // Geri gitme veya onay ekranına gitme için
                    serviceId = serviceId,
                    onBookingConfirmed = {
                        // Rezervasyon onaylandığında Confirmation ekranına git
                        // Booking ekranını stack'ten çıkarmak isteyebiliriz
                        navController.navigate(ScreenRoutes.BookingConfirmation.route) {
                            popUpTo(ScreenRoutes.ServiceList.route) // Servis listesine kadar geri dön
                            launchSingleTop = true // Eğer zaten oradaysa tekrar açma
                        }
                    }
                )
            } else {
                // serviceId gelmediyse bir hata durumu yönetilebilir veya geri gidilebilir
                // Şimdilik boş bırakabiliriz veya bir hata mesajı gösterebiliriz.
                // Loglama yapmak iyi olur.
                navController.popBackStack() // Otomatik geri git
            }
        }

        // BookingConfirmation Ekranı
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

        // Diğer ekranlar buraya eklenebilir...
    }
}