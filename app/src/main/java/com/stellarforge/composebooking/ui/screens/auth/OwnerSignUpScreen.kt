package com.stellarforge.composebooking.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SignUpViewEvent.NavigateTo -> {
                    navController.navigate(event.route) {
                        popUpTo(ScreenRoutes.OwnerLogin.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is SignUpViewEvent.ShowSnackbar -> {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(event.messageResId)) }
                }
            }
        }
    }

    SignUpScreenContent(
        uiState = uiState,
        screenTitle = stringResource(R.string.owner_signup_title),
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSignUpClick = viewModel::onSignUpClick,
        onNavigateBack = { navController.popBackStack() },
        snackbarHostState = snackbarHostState,
        linksContent = {
            Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 32.dp))
            TextButton(
                onClick = {
                    navController.navigate(ScreenRoutes.SignUp.route) {
                        popUpTo(ScreenRoutes.OwnerSignUp.route) { inclusive = true }
                    }
                }
            ) {
                Text(stringResource(id = R.string.signup_customer_link))
            }
        }
    )
}

