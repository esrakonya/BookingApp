package com.stellarforge.composebooking.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.utils.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Splash Screen.
 *
 * **Responsibilities:**
 * - **Startup Orchestration:** Manages the initial app launch sequence.
 * - **Branding Delay:** Enforces a minimum display time for the logo to prevent UI flickering on fast devices.
 * - **Route Determination:** Checks the user's session and role (Owner vs. Customer) to decide the start destination.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val userPrefs: UserPrefs
) : ViewModel() {

    // Holds the navigation target. Null means "Still Loading".
    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        initializeSplashScreen()
    }

    private fun initializeSplashScreen() {
        viewModelScope.launch {
            // 1. Minimum Branding Duration
            // We force the splash screen to stay visible for at least 1000ms (1 second).
            // This prevents the screen from flickering too fast on high-end devices
            // and ensures the user perceives the brand logo.
            val minSplashTime = System.currentTimeMillis() + 1000

            // 2. Determine Navigation Destination
            // STRATEGY: Offline-First / Caching (Best Practice)
            // Instead of waiting for a network call to Firestore (which might be slow),
            // we first check the local storage (SharedPreferences).
            val cachedRole = userPrefs.getUserRole()

            val destination = if (cachedRole != null) {
                // CACHE HIT: We know the role immediately.
                // This ensures instant app launch even without internet connection.
                if (cachedRole == "owner") {
                    ScreenRoutes.Schedule.route
                } else {
                    ScreenRoutes.ServiceList.route
                }
            } else {
                // CACHE MISS: First time install or data cleared.
                // Fallback to Network Check (slower but necessary for initial setup).
                checkUserStatus()
            }

            // 3. Handle Animation Timing
            // Calculate remaining time to meet the minimum duration requirement.
            val delayTime = minSplashTime - System.currentTimeMillis()
            if (delayTime > 0) {
                delay(delayTime)
            }

            // 4. Navigate
            _startDestination.value = destination
        }
    }

    /**
     * Checks if a user is logged in and determines their role.
     * @return The route string for the next screen.
     */
    private suspend fun checkUserStatus(): String {
        return when (val result = getCurrentUserUseCase()) {
            is Result.Success -> {
                val authUser = result.data
                if (authUser != null && authUser.uid.isNotBlank()) {
                    Timber.i("User authenticated. Role: ${authUser.role}")

                    // Role-Based Navigation Logic
                    if (authUser.role == "owner") {
                        ScreenRoutes.Schedule.route // Go to Admin Dashboard
                    } else {
                        ScreenRoutes.ServiceList.route // Go to Customer Storefront
                    }
                } else {
                    ScreenRoutes.Login.route // Go to Login
                }
            }
            is Result.Error -> {
                Timber.e(result.exception, "Auth check failed during splash.")
                ScreenRoutes.Login.route // Fail-safe: Go to Login
            }
            else -> ScreenRoutes.Login.route
        }
    }
}