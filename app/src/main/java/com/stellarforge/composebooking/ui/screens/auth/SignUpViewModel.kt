package com.stellarforge.composebooking.ui.screens.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.domain.usecase.SignUpUseCase
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

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    @StringRes val emailErrorRes: Int? = null,
    @StringRes val passwordErrorRes: Int? = null,
    @StringRes val confirmPasswordErrorRes: Int? = null,
    @StringRes val generalErrorRes: Int? = null
)

// Tek seferlik UI olayları (Login ile aynı olabilir veya farklılaşabilir)
sealed interface SignUpViewEvent {
    object NavigateToServiceList : SignUpViewEvent
    data class ShowSnackbar(@StringRes val messageResId: Int) : SignUpViewEvent
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SignUpViewEvent>()
    val eventFlow: SharedFlow<SignUpViewEvent> = _eventFlow.asSharedFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailErrorRes = null, generalErrorRes = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordErrorRes = null, generalErrorRes = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordErrorRes = null, generalErrorRes = null) }
    }

    fun onSignUpClick() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        if (validateInput(currentState)) {
            signUpUser(currentState.email, currentState.password)
        }
    }

    private fun validateInput(state: SignUpUiState): Boolean {
        var isValid = true
        var emailError: Int? = null
        var passwordError: Int? = null
        var confirmPasswordError: Int? = null

        // Email Validasyon
        if (state.email.isBlank()) {
            emailError = R.string.error_email_empty
            isValid = false
        } else if (!ValidationUtils.isEmailValid(state.email)) {
            emailError = R.string.error_email_invalid
            isValid = false
        }

        // Password Validasyon
        if (state.password.isBlank()) {
            passwordError = R.string.error_password_empty
            isValid = false
        } else if (!ValidationUtils.isPasswordLengthValid(state.password)) {
            passwordError = R.string.error_password_short
            isValid = false
        }
        // Daha karmaşık şifre kuralları (büyük/küçük harf, sayı vb.) buraya eklenebilir.

        // Confirm Password Validasyon
        if (state.confirmPassword.isBlank()) {
            confirmPasswordError = R.string.error_password_empty // Veya "Confirm password cannot be empty"
            isValid = false
        } else if (state.password != state.confirmPassword) {
            confirmPasswordError = R.string.error_password_mismatch
            isValid = false
        }

        // Hataları state'e yansıt
        if (emailError != state.emailErrorRes ||
            passwordError != state.passwordErrorRes ||
            confirmPasswordError != state.confirmPasswordErrorRes) {
            _uiState.update {
                it.copy(
                    emailErrorRes = emailError,
                    passwordErrorRes = passwordError,
                    confirmPasswordErrorRes = confirmPasswordError
                )
            }
        }

        return isValid
    }

    private fun signUpUser(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, generalErrorRes = null) }

        viewModelScope.launch {
            Timber.d("Attempting sign up for email: $email")
            val result = signUpUseCase(email, password)

            if (result.isSuccess) {
                Timber.i("Sign up successful for user: ${result.getOrNull()?.email}")
                _uiState.update { it.copy(isLoading = false) }
                _eventFlow.emit(SignUpViewEvent.NavigateToServiceList) // Başarılı, ana ekrana git
            } else {
                Timber.w(result.exceptionOrNull(), "Sign up failed for email: $email")
                val errorRes = when (result.exceptionOrNull()) {
                    is IllegalArgumentException -> R.string.error_signup_failed // Boş alan hatası vs.
                    is com.google.firebase.FirebaseNetworkException -> R.string.error_network_connection
                    is com.google.firebase.auth.FirebaseAuthUserCollisionException -> R.string.error_auth_email_collision
                    is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> R.string.error_auth_weak_password
                    else -> R.string.error_signup_failed // Diğer genel hatalar
                }
                _uiState.update { it.copy(isLoading = false, generalErrorRes = errorRes) }
                // _eventFlow.emit(SignUpViewEvent.ShowSnackbar(errorRes)) // Veya Snackbar ile
            }
        }
    }
}