package com.stellarforge.composebooking.ui.screens.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
 * The dedicated Registration screen for **Business Owners**.
 *
 * **Architecture Note:**
 * This screen utilizes the shared [SignUpScreenTemplate] to maintain UI consistency
 * with the Customer Registration flow, but implements distinct logic:
 * - Registers the user with the "owner" role.
 * - Navigates to the Owner Dashboard (Schedule) upon success.
 * - Provides navigation back to the Customer Sign-Up flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerSignUpScreen(
    navController: NavController,
    viewModel: OwnerSignUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Handle Side-effects (Navigation & Error Messages)
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SignUpViewEvent.NavigateTo -> {
                    navController.navigate(event.route) {
                        // Clear back stack to prevent returning to Login flow
                        popUpTo(ScreenRoutes.OwnerLogin.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is SignUpViewEvent.ShowSnackbar -> {
                    scope.launch {
                        // Use "error" label to trigger the Red styling in AppSnackbar
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

        // --- Text Resources ---
        title = stringResource(R.string.owner_signup_title), // "Create Business Account"
        subtitle = stringResource(R.string.owner_signup_subtitle), // "Start managing your business today."

        // --- Footer: Link to Customer Sign Up ---
        footerContent = {
            TextButton(
                onClick = {
                    navController.navigate(ScreenRoutes.SignUp.route) {
                        popUpTo(ScreenRoutes.OwnerSignUp.route) { inclusive = true }
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.signup_customer_link)) // "Want to sign up as customer?"
                }
            }
        }
    )
}