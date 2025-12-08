package com.stellarforge.composebooking.ui.screens.customerprofile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.domain.usecase.GetBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI State for the Customer Profile screen.
 *
 * @param userName The display name derived from email. If null, UI should show a default localized string.
 * @param errorMessageId Localized resource ID for error messages.
 */
data class CustomerProfileUiState(
    val isLoading: Boolean = false,
    val userName: String? = null,
    val userEmail: String = "",
    @StringRes val errorMessageId: Int? = null,
    val businessProfile: BusinessProfile? = null
)

sealed interface CustomerProfileEvent {
    object NavigateToLogin : CustomerProfileEvent
}

/**
 * ViewModel for the "My Account" screen.
 *
 * **Responsibilities:**
 * - Fetches current user details (Name, Email).
 * - **White-Label Feature:** Fetches the Business Profile (Logo, Address) using the constant [FirebaseConstants.TARGET_BUSINESS_OWNER_ID]
 *   to display contact info to the customer via a Dialog.
 * - Handles the Sign-Out process and navigation.
 */
@HiltViewModel
class CustomerProfileViewModel @Inject constructor(
    private val signOutUseCase: SignOutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getBusinessProfileUseCase: GetBusinessProfileUseCase
): ViewModel() {

    private val _uiState = MutableStateFlow(CustomerProfileUiState())
    val uiState: StateFlow<CustomerProfileUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CustomerProfileEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadUserProfile()
        loadBusinessContactInfo()
    }

    /**
     * Fetches the Business Profile to show Contact Info (Address/Phone) to the customer.
     */
    private fun loadBusinessContactInfo() {
        viewModelScope.launch {
            val targetId = FirebaseConstants.TARGET_BUSINESS_OWNER_ID

            // We listen to the flow to get updates if the owner changes info
            getBusinessProfileUseCase(targetId).collect { result ->
                if (result is Result.Success) {
                    _uiState.update {
                        it.copy(businessProfile = result.data)
                    }
                }
            }
        }
    }

    private fun loadUserProfile() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            when (val result = getCurrentUserUseCase()) {
                is Result.Success -> {
                    val user = result.data
                    if (user != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                // If email exists, take the part before '@' as the name.
                                // If null, the UI will display the default "Valued Customer" string.
                                userName = user.email?.substringBefore("@"),
                                userEmail = user.email ?: "",
                                errorMessageId = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessageId = R.string.error_user_not_found)
                        }
                    }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Error loading user profile")
                    _uiState.update {
                        it.copy(isLoading = false, errorMessageId = R.string.error_profile_load_failed)
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _eventFlow.emit(CustomerProfileEvent.NavigateToLogin)
        }
    }
}