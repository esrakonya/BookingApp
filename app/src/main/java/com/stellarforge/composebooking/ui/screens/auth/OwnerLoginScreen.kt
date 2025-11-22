package com.stellarforge.composebooking.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Email // E-posta ikonu
import androidx.compose.material.icons.filled.Lock // Kilit ikonu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource // Logo için
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R // R.drawable.your_app_logo için varsayımsal import
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import kotlinx.coroutines.flow.collectLatest

@Composable
fun OwnerLoginScreen(
    navController: NavController,
    viewModel: OwnerLoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is LoginViewEvent.NavigateTo -> {
                    navController.navigate(event.route) {
                        popUpTo(ScreenRoutes.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }

                is LoginViewEvent.ShowSnackbar -> {
                    val message = context.getString(event.messageId) // formatArgs yoktu event'te
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    LoginScreenContent(
        uiState = uiState,
        screenTitle = stringResource(R.string.owner_login_title),
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::onLoginClick,
        onNavigateBack = { navController.popBackStack() },
        snackbarHostState = snackbarHostState,
        linksContent = {
            TextButton(onClick = { navController.navigate(ScreenRoutes.OwnerSignUp.route) }) {
                Text("İşletme Hesabı Oluştur")
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 32.dp))
            TextButton(onClick = { navController.popBackStack() }) { // Geri dön
                Text("Müşteri misiniz? Geri Dön")
            }
        }
    )
}