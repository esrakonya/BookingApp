package com.stellarforge.composebooking.ui.screens.auth

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The dedicated Login screen for **Business Owners**.
 *
 * **Architecture Note:**
 * This screen reuses the [LoginScreenTemplate] to ensure design consistency with the
 * Customer Login screen, but injects Owner-specific logic and navigation paths.
 *
 * **Flow:**
 * - On Success: Navigates to the [ScreenRoutes.Schedule] (Admin Dashboard).
 * - On Failure: Shows an error via [AppSnackbarHost].
 * - Security: If a Customer tries to log in here, the [OwnerLoginViewModel] blocks access.
 */
@Composable
fun OwnerLoginScreen(
    navController: NavController,
    viewModel: OwnerLoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Handle One-time Events (Navigation & Errors)
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is LoginViewEvent.NavigateTo -> {
                    navController.navigate(event.route) {
                        // Clear back stack to prevent returning to Login
                        popUpTo(ScreenRoutes.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }

                is LoginViewEvent.ShowSnackbar -> {
                    scope.launch {
                        // Trigger the Red/Error style in AppSnackbar by setting actionLabel to "error"
                        snackbarHostState.showSnackbar(
                            message = context.getString(event.messageId),
                            actionLabel = "error",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    // Render UI using the shared Template
    LoginScreenTemplate(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::onLoginClick,
        snackbarHostState = snackbarHostState,

        // --- Owner Specific Header ---
        title = stringResource(R.string.owner_login_title), // "Business Login"
        subtitle = stringResource(R.string.owner_login_subtitle), // "Manage your business efficiently."

        // --- Footer Actions ---

        // Primary: Navigate to Owner Registration
        primaryFooterText = stringResource(R.string.signup_owner_link), // "Create Business Account"
        onPrimaryFooterClick = { navController.navigate(ScreenRoutes.OwnerSignUp.route) },

        // Secondary: Return to Customer Login (Role Switch)
        secondaryFooterText = stringResource(R.string.login_back_to_customer), // "Back to Customer Login"
        onSecondaryFooterClick = { navController.popBackStack() },

        // Back button enabled to easily return to main entrance
        onNavigateBack = { navController.popBackStack() }
    )
}