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
import com.stellarforge.composebooking.R // R.drawable.your_app_logo için varsayımsal import

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val eventFlow = viewModel.eventFlow
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(key1 = Unit) {
        eventFlow.collect { event ->
            when (event) {
                is LoginViewEvent.NavigateToServiceList -> {
                    onLoginSuccess()
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        // Login ekranı için TopAppBar genellikle kullanılmaz.
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp) // Yan boşluklar arttırıldı, dikey padding eklendi
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Opsiyonel: Uygulama Logosu veya Adı
            // painterResource için drawable klasörünüzde bir resim olmalı (örn: your_app_logo.png)
            // Image(
            //     painter = painterResource(id = R.drawable.your_app_logo), // Kendi logonuzun ID'si
            //     contentDescription = stringResource(id = R.string.app_name),
            //     modifier = Modifier
            //         .size(100.dp) // Logonun boyutunu ayarlayın
            //         .padding(bottom = 24.dp)
            // )
            // VEYA sadece stilize bir uygulama adı:
            Text(
                text = stringResource(id = R.string.app_name), // Uygulama adınız
                style = MaterialTheme.typography.displaySmall, // Daha büyük ve dikkat çekici
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(id = R.string.login_title),
                style = MaterialTheme.typography.headlineMedium, // Biraz küçültüldü, app_name daha büyükse
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Giriş Formu Alanı
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large, // Daha yuvarlak köşeler
                tonalElevation = 2.dp // Hafif bir yükselti
            ) {
                Column(modifier = Modifier.padding(all = 24.dp)) { // İç padding arttırıldı
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(id = R.string.auth_email_label)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null
                            )
                        },
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
                            uiState.emailErrorRes?.let {
                                Text(stringResource(id = it), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(id = R.string.auth_password_label)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null
                            )
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (!uiState.isLoading) { // Yükleme yoksa butona tıkla
                                    viewModel.onLoginClick()
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
                        isError = uiState.passwordErrorRes != null,
                        supportingText = {
                            uiState.passwordErrorRes?.let {
                                Text(stringResource(id = it), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp)) // Butonla TextField arası boşluk

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.onLoginClick()
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(id = R.string.login_button_text))
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

            Spacer(modifier = Modifier.height(24.dp))

            ClickableText(
                text = AnnotatedString(stringResource(id = R.string.login_navigate_to_signup_text)),
                onClick = { onNavigateToSignUp() },
                style = MaterialTheme.typography.bodyLarge.copy( // Biraz daha büyük
                    color = MaterialTheme.colorScheme.primary, // Temanın ana rengi
                    textAlign = TextAlign.Center
                )
            )
            Spacer(modifier = Modifier.height(16.dp)) // En altta biraz daha boşluk
        }
    }
}

// strings.xml'e eklenmesi gerekenler:
// <string name="auth_show_password_desc">Show password</string>
// <string name="auth_hide_password_desc">Hide password</string>
// <drawable name="your_app_logo">...</drawable> (Eğer logo kullanacaksanız)