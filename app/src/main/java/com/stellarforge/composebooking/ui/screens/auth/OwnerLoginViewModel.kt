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

/**
 * Manages the UI state for the **Business Owner Login** screen.
 *
 * **Responsibilities:**
 * - Authentication: handling email/password input and validation.
 * - **Security / Role Enforcement:** Strictly checks if the authenticated user has the "owner" role.
 *   If a "customer" tries to log in via this portal, access is denied and the session is cleared.
 * - Error Handling: Maps Firebase exceptions to user-friendly UI messages.
 */
@HiltViewModel
class OwnerLoginViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<LoginViewEvent>()
    val eventFlow: SharedFlow<LoginViewEvent> = _eventFlow.asSharedFlow()

    init {
        Timber.d("OwnerLoginViewModel initialized.")
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailErrorRes = null, generalErrorRes = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordErrorRes = null, generalErrorRes = null) }
    }

    fun onLoginClick() {
        val currentState = _uiState.value
        // Prevent duplicate requests
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

        // Email Validation
        val newEmailError: Int? = if (state.email.isBlank()) {
            isValid = false
            R.string.error_email_empty
        } else if (!ValidationUtils.isEmailValid(state.email)) {
            isValid = false
            R.string.error_email_invalid
        } else {
            null
        }

        // Password Validation
        val newPasswordError: Int? = if (state.password.isBlank()) {
            isValid = false
            R.string.error_password_empty
        } else {
            null
        }

        // Only update state if errors changed
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
            Timber.d("Attempting owner sign in for: $email")

            when (val result = signInUseCase(email, password)) {
                is Result.Success -> {
                    val user = result.data

                    // --- SECURITY: ROLE CHECK ---
                    if (user.role == "owner") {
                        // Correct role -> Access Granted -> Navigate to Dashboard
                        Timber.i("Owner sign in successful for: ${user.uid}")
                        _uiState.update { it.copy(isLoading = false) }
                        _eventFlow.emit(LoginViewEvent.NavigateTo(ScreenRoutes.Schedule.route))
                    } else {
                        // Incorrect role (Customer tried to use Owner portal) -> Access Denied
                        Timber.w("Security Alert: Customer (role=${user.role}) attempted to sign in via owner login.")
                        _uiState.update { it.copy(isLoading = false) }
                        _eventFlow.emit(LoginViewEvent.ShowSnackbar(R.string.error_auth_customer_at_owner_login))

                        // Force sign out to clear the invalid session
                        signOutUseCase()
                    }
                }
                is Result.Error -> {
                    Timber.w(result.exception, "Owner sign in failed. Message: ${result.message}")
                    val errorRes = when (result.exception) {
                        is FirebaseNetworkException -> R.string.error_network_connection
                        is FirebaseAuthInvalidCredentialsException -> R.string.error_auth_invalid_credentials
                        is FirebaseAuthInvalidUserException -> R.string.error_auth_invalid_credentials
                        is IllegalArgumentException -> R.string.error_auth_generic_signup
                        else -> R.string.error_login_failed
                    }
                    _uiState.update { it.copy(isLoading = false, generalErrorRes = errorRes) }
                }
                is Result.Loading -> {
                    // Should not happen for this UseCase
                }
            }
        }
    }
}