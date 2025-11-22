package com.stellarforge.composebooking.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.domain.usecase.SignUpUseCase
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import com.stellarforge.composebooking.utils.Result
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

@HiltViewModel
class OwnerSignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase
): ViewModel() {
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
        if (currentState.isLoading) {
            Timber.d("Sign up attempt while already loading. Ignoring.")
            return
        }

        if (validateInput(currentState)) {
            signUpUser(currentState.email, currentState.password)
        }
    }

    private fun validateInput(state: SignUpUiState): Boolean {
        var isValid = true
        val newEmailError: Int? = if (state.email.isBlank()) {
            isValid = false
            R.string.error_email_empty
        } else if (!ValidationUtils.isEmailValid(state.email)) {
            isValid = false
            R.string.error_email_invalid
        } else {
            null
        }

        val newPasswordError: Int? = if (state.password.isBlank()) {
            isValid = false
            R.string.error_password_empty
        } else if (!ValidationUtils.isPasswordLengthValid(state.password)) { // Şifre uzunluk validasyonu
            isValid = false
            R.string.error_password_short
        } else {
            null
        }

        val newConfirmPasswordError: Int? = if (state.confirmPassword.isBlank()) {
            isValid = false
            R.string.error_confirm_password_empty // Ayrı bir string kaynağı kullanmak daha iyi olabilir
        } else if (state.password != state.confirmPassword) {
            isValid = false
            R.string.error_password_mismatch
        } else {
            null
        }

        // Sadece state değiştiyse güncelleme yapalım
        if (newEmailError != state.emailErrorRes ||
            newPasswordError != state.passwordErrorRes ||
            newConfirmPasswordError != state.confirmPasswordErrorRes) {
            _uiState.update {
                it.copy(
                    emailErrorRes = newEmailError,
                    passwordErrorRes = newPasswordError,
                    confirmPasswordErrorRes = newConfirmPasswordError
                )
            }
        }
        return isValid
    }

    private fun signUpUser(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, generalErrorRes = null) }

        viewModelScope.launch {
            Timber.d("Attempting sign up for email: $email as an OWNER")
            // SignUpUseCase'in `suspend operator fun invoke(...): Result<AuthUser>` döndürdüğünü varsayıyoruz.
            when (val result = signUpUseCase(email, password, role = "owner")) {
                is Result.Success -> {
                    // Başarılı kayıt, result.data AuthUser nesnesini içerir.
                    Timber.i("Owner sign up successful for user: ${result.data.uid} - ${result.data.email}")
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(SignUpViewEvent.NavigateTo(route = ScreenRoutes.Schedule.route))
                }
                is Result.Error -> {
                    // Hatalı kayıt, result.exception ve result.message kullanılabilir
                    Timber.w(result.exception, "Sign up failed for email: $email. Message: ${result.message}")
                    val errorRes = when (result.exception) {
                        is FirebaseNetworkException -> R.string.error_network_connection
                        is FirebaseAuthUserCollisionException -> R.string.error_auth_email_collision // E-posta zaten kullanımda
                        is FirebaseAuthWeakPasswordException -> R.string.error_auth_weak_password // Zayıf şifre
                        is IllegalArgumentException -> R.string.error_auth_generic_signup // Validasyon hatası UseCase'den geliyorsa
                        else -> R.string.error_signup_failed // Diğer genel hatalar
                    }
                    _uiState.update { it.copy(isLoading = false, generalErrorRes = errorRes) }
                }
                is Result.Loading -> {
                    // Bu case'in `suspend fun` bir UseCase için çalışması beklenmez.
                    Timber.d("SignUpUseCase returned Loading state (unexpected for a direct suspend function result).")
                }
            }
        }
    }
}