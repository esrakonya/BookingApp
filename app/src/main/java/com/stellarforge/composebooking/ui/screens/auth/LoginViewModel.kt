package com.stellarforge.composebooking.ui.screens.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.domain.usecase.SignInUseCase
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
        if (currentState.isLoading) {
            Timber.d("Login attempt while already loading. Ignoring.")
            return
        }

        if (validateInput(currentState)) {
            signInUser(currentState.email, currentState.password)
        }
    }

    private fun validateInput(state: LoginUiState): Boolean {
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
        } else {
            null
        }

        // Sadece state değiştiyse güncelleme yapalım
        if (newEmailError != state.emailErrorRes || newPasswordError != state.passwordErrorRes) {
            _uiState.update {
                it.copy(emailErrorRes = newEmailError, passwordErrorRes = newPasswordError)
            }
        }
        return isValid
    }

    private fun signInUser(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, generalErrorRes = null) }

        viewModelScope.launch {
            Timber.d("Attempting sign in for email: $email")
            // SignInUseCase'in `suspend operator fun invoke(...): Result<AuthUser>` döndürdüğünü varsayıyoruz.
            when (val result = signInUseCase(email, password)) {
                is Result.Success -> {
                    // Başarılı giriş, result.data AuthUser nesnesini içerir.
                    // Bu data'nın null olup olmadığını kontrol etmeye gerek yok,
                    // çünkü SignInUseCase'den Result<AuthUser> (nullable olmayan AuthUser) bekliyoruz.
                    Timber.i("Sign in successful for user: ${result.data.uid} - ${result.data.email}")
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(LoginViewEvent.NavigateToServiceList)
                }
                is Result.Error -> {
                    // Hatalı giriş, result.exception ve result.message kullanılabilir
                    Timber.w(result.exception, "Sign in failed for email: $email. Message: ${result.message}")
                    val errorRes = when (result.exception) {
                        is FirebaseNetworkException -> R.string.error_network_connection
                        is FirebaseAuthInvalidCredentialsException -> R.string.error_auth_invalid_credentials
                        is FirebaseAuthInvalidUserException -> R.string.error_auth_user_not_found // Veya yine invalid_credentials
                        is IllegalArgumentException -> R.string.error_auth_generic_signup // Validasyon hatası UseCase'den geliyorsa
                        else -> R.string.error_login_failed // Diğer genel hatalar
                    }
                    _uiState.update { it.copy(isLoading = false, generalErrorRes = errorRes) }
                }
                is Result.Loading -> {
                    // Bu case'in `suspend fun` bir UseCase için çalışması beklenmez.
                    // UseCase ya Success ya da Error döndürmelidir.
                    // Eğer UseCase'iniz Flow döndürüyorsa bu durum geçerli olurdu.
                    Timber.d("SignInUseCase returned Loading state (unexpected for a direct suspend function result).")
                    // UI'da zaten isLoading = true olduğu için ek bir güncelleme gerekmeyebilir.
                }
            }
        }
    }
}