package com.stellarforge.composebooking.ui.screens.auth

import androidx.annotation.StringRes
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

sealed interface SignUpViewEvent {
    data class NavigateTo(val route: String) : SignUpViewEvent
    data class ShowSnackbar(@StringRes val messageResId: Int) : SignUpViewEvent
}

/**
 * Manages the UI state for the **Customer Registration** screen.
 *
 * **Responsibilities:**
 * - Handles user input for Email, Password, and Password Confirmation.
 * - **Role Assignment:** Automatically registers the user with the **"customer"** role via [SignUpUseCase].
 * - **Navigation:** Upon success, directs the user to the Customer Home Screen ([ScreenRoutes.ServiceList]).
 * - **Validation:** Ensures data integrity before making network calls.
 */
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
        // Prevent multiple clicks while loading
        if (currentState.isLoading) {
            Timber.d("Sign up attempt while loading. Ignored.")
            return
        }

        if (validateInput(currentState)) {
            signUpUser(currentState.email, currentState.password)
        }
    }

    private fun validateInput(state: SignUpUiState): Boolean {
        var isValid = true

        // 1. Email Validation
        val newEmailError: Int? = if (state.email.isBlank()) {
            isValid = false
            R.string.error_email_empty
        } else if (!ValidationUtils.isEmailValid(state.email)) {
            isValid = false
            R.string.error_email_invalid
        } else {
            null
        }

        // 2. Password Validation
        val newPasswordError: Int? = if (state.password.isBlank()) {
            isValid = false
            R.string.error_password_empty
        } else if (!ValidationUtils.isPasswordLengthValid(state.password)) {
            isValid = false
            R.string.error_password_short
        } else {
            null
        }

        // 3. Confirm Password Validation
        val newConfirmPasswordError: Int? = if (state.confirmPassword.isBlank()) {
            isValid = false
            R.string.error_password_empty
        } else if (state.password != state.confirmPassword) {
            isValid = false
            R.string.error_password_mismatch
        } else {
            null
        }

        // Update state only if errors changed
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
            Timber.d("Attempting Customer sign up for: $email")

            // Explicitly pass role = "customer"
            when (val result = signUpUseCase(email, password, role = "customer")) {
                is Result.Success -> {
                    Timber.i("Customer registered successfully: ${result.data.uid}")
                    _uiState.update { it.copy(isLoading = false) }
                    // Navigate to Home
                    _eventFlow.emit(SignUpViewEvent.NavigateTo(route = ScreenRoutes.ServiceList.route))
                }
                is Result.Error -> {
                    Timber.w(result.exception, "Sign up failed. Message: ${result.message}")
                    val errorRes = when (result.exception) {
                        is FirebaseNetworkException -> R.string.error_network_connection
                        is FirebaseAuthUserCollisionException -> R.string.error_auth_email_collision
                        is FirebaseAuthWeakPasswordException -> R.string.error_auth_weak_password
                        is IllegalArgumentException -> R.string.error_auth_generic_signup
                        else -> R.string.error_signup_failed
                    }
                    _uiState.update { it.copy(isLoading = false, generalErrorRes = errorRes) }
                }
                is Result.Loading -> {
                    // Should not happen
                }
            }
        }
    }
}