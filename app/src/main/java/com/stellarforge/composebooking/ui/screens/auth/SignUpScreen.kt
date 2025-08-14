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
import com.stellarforge.composebooking.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel = hiltViewModel(),
    onSignUpSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val eventFlow = viewModel.eventFlow
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(key1 = Unit) {
        eventFlow.collect { event ->
            when (event) {
                is SignUpViewEvent.NavigateToServiceList -> onSignUpSuccess()
                is SignUpViewEvent.ShowSnackbar -> {
                    val message = context.getString(event.messageResId) // formatArgs yoktu event'te
                    snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.signup_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button_desc)
                        )
                    }
                }
                // Opsiyonel: TopAppBar renkleri
                // colors = TopAppBarDefaults.topAppBarColors(
                //    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) // Hafif yükselti hissi
                // )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp) // Yan boşluklar arttırıldı
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // İçeriği yukarıdan başlat, form uzunsa kaydırılsın
        ) {
            // "Create your account" gibi bir alt başlık eklenebilir
            Text(
                text = stringResource(id = R.string.signup_subtitle), // strings.xml'e ekle: <string name="signup_subtitle">Create your account</string>
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(all = 24.dp)) {
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(id = R.string.auth_email_label)) },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        isError = uiState.emailErrorRes != null,
                        supportingText = {
                            uiState.emailErrorRes?.let { Text(stringResource(id = it)) }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(id = R.string.auth_password_label)) },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        trailingIcon = {
                            // passwordVisible true ise, şifre görünür demektir.
                            // Bu durumda "şifreyi gizle" ikonunu (gözü kapalı) gösteririz.
                            val image = if (passwordVisible)
                                Icons.Filled.VisibilityOff
                            else
                                Icons.Filled.Visibility

                            val description = if (passwordVisible)
                                stringResource(R.string.auth_hide_password_desc)
                            else
                                stringResource(R.string.auth_show_password_desc)

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        },
                        singleLine = true,
                        isError = uiState.passwordErrorRes != null,
                        supportingText = {
                            uiState.passwordErrorRes?.let { Text(stringResource(id = it)) }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(id = R.string.auth_confirm_password_label)) },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (!uiState.isLoading) {
                                    viewModel.onSignUpClick()
                                }
                            }
                        ),
                        trailingIcon = {
                            // passwordVisible true ise, şifre görünür demektir.
                            // Bu durumda "şifreyi gizle" ikonunu (gözü kapalı) gösteririz.
                            val image = if (passwordVisible)
                                Icons.Filled.Visibility
                            else
                                Icons.Filled.VisibilityOff

                            val description = if (passwordVisible)
                                stringResource(R.string.auth_hide_password_desc)
                            else
                                stringResource(R.string.auth_show_password_desc)

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = description)
                            }
                        },
                        singleLine = true,
                        isError = uiState.confirmPasswordErrorRes != null,
                        supportingText = {
                            uiState.confirmPasswordErrorRes?.let { Text(stringResource(id = it)) }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp)) // Butonla TextField arası boşluk

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.onSignUpClick()
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(id = R.string.signup_button_text))
                        }
                    }
                }
            }

            // Genel Hata Mesajı (Formun dışında, altında)
            if (uiState.generalErrorRes != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = uiState.generalErrorRes!!),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp)) // En altta biraz boşluk
        }
    }
}

// strings.xml'e eklenmesi gereken:
// <string name="signup_subtitle">Create your account</string>
// (auth_show_password_desc ve auth_hide_password_desc LoginScreen'den zaten gelmiş olmalı)