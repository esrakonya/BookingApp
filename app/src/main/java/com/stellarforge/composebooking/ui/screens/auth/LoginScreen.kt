package com.stellarforge.composebooking.ui.screens.auth

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
 * The main entry point for the **Customer Login** screen.
 *
 * This Composable acts as a "Controller" layer that:
 * 1. Connects to the [LoginViewModel].
 * 2. Observes state changes and one-time events (Navigation, Errors).
 * 3. Configures the reusable [LoginScreenTemplate] with Customer-specific texts and navigation actions.
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe one-time events from ViewModel (Navigation or Snackbar messages)
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when(event) {
                is LoginViewEvent.NavigateTo -> {
                    navController.navigate(event.route) {
                        // Clear back stack to prevent returning to Login
                        popUpTo(ScreenRoutes.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is LoginViewEvent.ShowSnackbar -> {
                    scope.launch {
                        // We use "error" as actionLabel to trigger the Red color in AppSnackbar
                        snackbarHostState.showSnackbar(
                            message = context.getString(event.messageId),
                            actionLabel = "error",
                            withDismissAction = true
                        )
                    }
                }
            }
        }
    }

    // Render the UI using the shared Template
    LoginScreenTemplate(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::onLoginClick,
        snackbarHostState = snackbarHostState,

        // --- Customer Specific Header ---
        title = stringResource(R.string.login_welcome_title),
        subtitle = stringResource(R.string.login_welcome_subtitle),

        // --- Footer Actions ---

        // Primary: Navigate to Customer Registration
        primaryFooterText = stringResource(R.string.login_navigate_to_signup_text),
        onPrimaryFooterClick = { navController.navigate(ScreenRoutes.SignUp.route) },

        // Secondary: Switch to Business Owner Login
        secondaryFooterText = stringResource(R.string.login_owner_link),
        onSecondaryFooterClick = { navController.navigate(ScreenRoutes.OwnerLogin.route) },

        // No back button on the starting screen
        onNavigateBack = null
    )
}