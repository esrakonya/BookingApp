package com.stellarforge.composebooking.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.components.AppSnackbarHost

/**
 * A Reusable, Professional Login Screen Template.
 *
 * **Purpose:**
 * This Composable serves as the "Single Source of Truth" for the Login UI layout.
 * It is designed to be used by both the **Customer Login** (`LoginScreen`) and the
 * **Owner Login** (`OwnerLoginScreen`) flows.
 *
 * **Features:**
 * - **Unified Design:** Ensures a consistent look and feel across different user roles.
 * - **Modular Footer:** Allows customization of bottom links (e.g., "Sign Up" vs "Switch Role") via parameters.
 * - **Error Handling:** Integrates inline error support for text fields and a general error message area.
 * - **Custom Snackbar:** Uses [AppSnackbarHost] for styled success/error notifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenTemplate(
    // --- DATA & STATE ---
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,

    // --- HEADER CONFIG ---
    title: String,
    subtitle: String,

    // --- FOOTER ACTIONS (Structured) ---
    primaryFooterText: String, // e.g., "Don't have an account? Sign Up"
    onPrimaryFooterClick: () -> Unit,

    secondaryFooterText: String, // e.g., "Business Login"
    onSecondaryFooterClick: () -> Unit,

    // --- NAVIGATION ---
    onNavigateBack: (() -> Unit)? = null,

    // --- SYSTEM ---
    snackbarHostState: SnackbarHostState
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // Show Back button only if navigation callback is provided
            if (onNavigateBack != null) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.action_navigate_back)
                            )
                        }
                    }
                )
            }
        },
        // USE CUSTOM SNACKBAR HOST HERE
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Centers content vertically
        ) {

            // --- 1. HEADER SECTION ---
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // --- 2. FORM SECTION ---

            // Email Input
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.auth_email_label)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                isError = uiState.emailErrorRes != null,
                supportingText = uiState.emailErrorRes?.let { { Text(stringResource(it)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.auth_password_label)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                singleLine = true,
                isError = uiState.passwordErrorRes != null,
                supportingText = uiState.passwordErrorRes?.let { { Text(stringResource(it)) } },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    val description = if (passwordVisible) R.string.auth_hide_password_desc else R.string.auth_show_password_desc
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(image, contentDescription = stringResource(description))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onLoginClick()
                }),
                shape = MaterialTheme.shapes.medium
            )

            // General Error Message (e.g., Network Error, Invalid Credentials)
            if (uiState.generalErrorRes != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stringResource(id = uiState.generalErrorRes),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = onLoginClick,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.login_button_text),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- 3. FOOTER SECTION ---

            // Primary Link (Sign Up)
            TextButton(onClick = onPrimaryFooterClick) {
                Text(
                    text = primaryFooterText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Divider
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "OR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Link (Role Switch)
            OutlinedButton(
                onClick = onSecondaryFooterClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = secondaryFooterText)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}