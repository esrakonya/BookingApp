package com.stellarforge.composebooking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.stellarforge.composebooking.ui.navigation.AppNavigation
import com.stellarforge.composebooking.ui.theme.ComposeBookingTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The Single Activity entry point for the application.
 *
 * **Responsibilities:**
 * - **Splash Screen Handling:** Initializes the Android 12+ Splash API and seamlessly transitions to the Compose UI.
 * - **Dependency Injection:** annotated with [@AndroidEntryPoint] to serve as the Hilt root for UI components.
 * - **Theme & Navigation:** Applies the global [ComposeBookingTheme] and hosts the [AppNavigation] graph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install System Splash Screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 2. Seamless Transition Strategy:
        // We return 'false' immediately. This tells the System Splash to disappear
        // as soon as the app is ready, allowing our animated [SplashScreen] (in Compose)
        // to take over instantly without a "white screen" gap.
        splashScreen.setKeepOnScreenCondition { false }

        setContent {
            ComposeBookingTheme {
                // A Surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 3. Initialize Navigation
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }
            }
        }
    }
}