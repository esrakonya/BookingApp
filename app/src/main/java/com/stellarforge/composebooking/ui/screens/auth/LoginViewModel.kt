package com.stellarforge.composebooking.ui.screens.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.domain.usecase.SignInUseCase
import com.stellarforge.composebooking.utils.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    @StringRes val emailErrorRes: Int? = null,
    @StringRes val passwordErrorRes: Int? = null,
    @StringRes val generalErrorRes: Int? = null
)

sealed interface LoginViewEvent {
    object NavigateToServiceList : LoginViewEvent
    data class ShowSnackbar(@StringRes val messageId: Int): LoginViewEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<LoginViewEvent>()
    val eventFlow: SharedFlow<LoginViewEvent> = _eventFlow.asSharedFlow()

    init {
        Timber.d("LoginViewModel initialized.")
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailErrorRes = null, generalErrorRes = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordErrorRes = null, generalErrorRes = null) }
    }

    fun onLoginClick() {
        val currentState = _uiState.value
        if (currentState.isLoading) return // Zaten işlem yapılıyorsa tekrar tetikleme

        if (validateInput(currentState)) {
            signInUser(currentState.email, currentState.password)
        }
    }

    private fun validateInput(state: LoginUiState): Boolean {
        var isValid = true
        var emailError: Int? = null
        var passwordError: Int? = null

        if (state.email.isBlank()) {
            emailError = R.string.error_email_empty
            isValid = false
        } else if (!ValidationUtils.isEmailValid(state.email)) {
            // Domain'den çıkardığımız validasyon burada
            emailError = R.string.error_email_invalid
            isValid = false
        }

        if (state.password.isBlank()) {
            passwordError = R.string.error_password_empty
            isValid = false
        }
        // Giriş için şifre uzunluğu kontrolü genellikle yapılmaz,
        // ama istenirse eklenebilir.

        // Hataları state'e yansıt (sadece değiştiyse güncelleme)
        if (emailError != state.emailErrorRes || passwordError != state.passwordErrorRes) {
            _uiState.update { it.copy(emailErrorRes = emailError, passwordErrorRes = passwordError) }
        }

        return isValid
    }

    private fun signInUser(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, generalErrorRes = null) } // Yükleme başlasın, eski genel hatayı temizle

        viewModelScope.launch {
            Timber.d("Attempting sign in for email: $email")
            val result = signInUseCase(email, password)

            if (result.isSuccess) {
                Timber.i("Sign in successful for user: ${result.getOrNull()?.email}")
                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(LoginViewEvent.NavigateToServiceList) // Başarılı, ana ekrana git
            } else {
                Timber.w(result.exceptionOrNull(), "Sign in failed for email: $email")
                val errorRes = when (result.exceptionOrNull()) {
                    is IllegalArgumentException -> R.string.error_auth_invalid_credentials // Boş alan hatası vs.
                    is com.google.firebase.FirebaseNetworkException -> R.string.error_network_connection
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> R.string.error_auth_invalid_credentials
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> R.string.error_auth_invalid_credentials // Kullanıcı yok veya şifre yanlış
                    else -> R.string.error_login_failed // Diğer genel hatalar
                }
                _uiState.update { it.copy(isLoading = false, generalErrorRes = errorRes) }
                // Alternatif olarak Snackbar ile de gösterilebilir:
                // _eventFlow.emit(LoginViewEvent.ShowSnackbar(errorRes))
            }
        }
    }
}