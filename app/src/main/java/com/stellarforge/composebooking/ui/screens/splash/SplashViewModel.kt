package com.stellarforge.composebooking.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import com.stellarforge.composebooking.utils.Result
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
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    // Holds the navigation target. Null means "Still Loading".
    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        initializeSplashScreen()
    }

    private fun initializeSplashScreen() {
        viewModelScope.launch {
            // 1. Minimum delay for branding visibility (2 seconds)
            // Ensures the user sees the logo even if the auth check is instant.
            val minSplashTime = System.currentTimeMillis() + 2000

            // 2. Verify User Session
            val destination = checkUserStatus()

            // 3. Wait for the remaining time if logic finished too fast
            val delayTime = minSplashTime - System.currentTimeMillis()
            if (delayTime > 0) {
                delay(delayTime)
            }

            // 4. Set navigation destination (Triggers UI navigation)
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