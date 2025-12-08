package com.stellarforge.composebooking.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The main entry point for the **Customer Registration** screen.
 *
 * This Composable orchestrates the sign-up flow for customers:
 * 1. Observes UI state from [SignUpViewModel].
 * 2. Handles navigation events (Success -> Home).
 * 3. Displays error messages using the custom [AppSnackbarHost].
 * 4. Delegates the UI rendering to the reusable [SignUpScreenTemplate].
 */
@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe Side-effects
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SignUpViewEvent.NavigateTo -> {
                    navController.navigate(event.route) {
                        // Clear back stack to prevent returning to Login
                        popUpTo(ScreenRoutes.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is SignUpViewEvent.ShowSnackbar -> {
                    scope.launch {
                        // Use "error" action label to trigger Red Snackbar style
                        snackbarHostState.showSnackbar(
                            message = context.getString(event.messageResId),
                            actionLabel = "error",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    // Render UI using the shared Template
    SignUpScreenTemplate(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSignUpClick = viewModel::onSignUpClick,
        onNavigateBack = { navController.popBackStack() },
        snackbarHostState = snackbarHostState,

        // --- Customer Specific Text ---
        title = stringResource(id = R.string.signup_title),
        subtitle = stringResource(id = R.string.signup_subtitle), // "Create your account"

        // --- Footer: Link to Owner Sign Up ---
        footerContent = {
            TextButton(
                onClick = {
                    navController.navigate(ScreenRoutes.OwnerSignUp.route) {
                        // Switch flows cleanly
                        popUpTo(ScreenRoutes.SignUp.route) { inclusive = true }
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.signup_owner_link)) // "Create Business Account"
                }
            }
        }
    )
}