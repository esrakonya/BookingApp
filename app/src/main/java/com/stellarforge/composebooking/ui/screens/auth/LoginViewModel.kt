package com.stellarforge.composebooking.ui.screens.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.domain.usecase.SignInUseCase
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    @StringRes val emailErrorRes: Int? = null,
    @StringRes val passwordErrorRes: Int? = null,
    @StringRes val generalErrorRes: Int? = null
)

sealed interface LoginViewEvent {
    data class NavigateTo(val route: String) : LoginViewEvent
    data class ShowSnackbar(@StringRes val messageId: Int): LoginViewEvent
}

/**
 * Manages the UI state for the Customer Login screen.
 *
 * **Responsibilities:**
 * - Handles email/password input and validation.
 * - Executes login via [SignInUseCase].
 * - **Role Verification:** Enforces security by checking if the logged-in user has the "customer" role.
 * - Maps Firebase exceptions to user-friendly error messages (Resource IDs).
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signOutUseCase: SignOutUseCase
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

        // Only update state if the error values have actually changed to avoid unnecessary recompositions
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
            when (val result = signInUseCase(email, password)) {
                is Result.Success -> {
                    val user = result.data

                    // --- SECURITY: ROLE CHECK ---
                    if (user.role == "customer") {
                        // Correct role, proceed to home
                        Timber.i("Customer sign in successful for: ${user.uid}")
                        _uiState.update { it.copy(isLoading = false) }
                        _eventFlow.emit(LoginViewEvent.NavigateTo(ScreenRoutes.ServiceList.route))
                    } else {
                        // Incorrect role (Owner tried to login via Customer portal)
                        Timber.w("Security Alert: Owner (role=${user.role}) attempted to sign in via customer login.")
                        _uiState.update { it.copy(isLoading = false) }
                        _eventFlow.emit(LoginViewEvent.ShowSnackbar(R.string.error_auth_owner_at_customer_login))

                        // Force sign out to clear the invalid session
                        signOutUseCase()
                    }
                }
                is Result.Error -> {
                    Timber.w(result.exception, "Sign in failed for email: $email. Message: ${result.message}")
                    val errorRes = when (result.exception) {
                        is FirebaseNetworkException -> R.string.error_network_connection
                        is FirebaseAuthInvalidCredentialsException -> R.string.error_auth_invalid_credentials
                        is FirebaseAuthInvalidUserException -> R.string.error_auth_invalid_credentials // Treat as invalid credentials for security
                        is IllegalArgumentException -> R.string.error_auth_generic_signup // Validation error from UseCase
                        else -> R.string.error_login_failed // Generic fallback
                    }
                    _uiState.update { it.copy(isLoading = false, generalErrorRes = errorRes) }
                    _eventFlow.emit(LoginViewEvent.ShowSnackbar(errorRes))
                }
                is Result.Loading -> {
                    Timber.d("SignInUseCase returned Loading state (unexpected for a direct suspend function).")
                }
            }
        }
    }
}