package com.stellarforge.composebooking.ui.screens.customerprofile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.domain.usecase.GetBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetCustomerProfileUseCase
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Represents the UI state for the Customer Profile screen.
 *
 * @property isLoading Indicates if data is currently being fetched.
 * @property userName The display name of the customer (fetched from Firestore).
 * @property userEmail The email of the customer (fetched from Auth).
 * @property errorMessageId Localized resource ID for error messages.
 * @property businessProfile Holds the shop's contact info (Address, Phone) to be displayed in the "Contact Us" dialog.
 */
data class CustomerProfileUiState(
    val isLoading: Boolean = false,
    val userName: String? = null,
    val userEmail: String = "",
    @StringRes val errorMessageId: Int? = null,
    val businessProfile: BusinessProfile? = null
)

/**
 * One-time events for the Customer Profile screen (e.g., Navigation).
 */
sealed interface CustomerProfileEvent {
    object NavigateToLogin : CustomerProfileEvent
}

/**
 * ViewModel for the "My Account" screen.
 *
 * **Responsibilities:**
 * - Fetches the current authenticated user's details (Real-time stream).
 * - Fetches the Business Profile (White-Label Config) to display store contact info.
 * - Handles the Sign-Out process.
 */
@HiltViewModel
class CustomerProfileViewModel @Inject constructor(
    private val signOutUseCase: SignOutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getCustomerProfileUseCase: GetCustomerProfileUseCase,
    private val getBusinessProfileUseCase: GetBusinessProfileUseCase
): ViewModel() {

    private val _uiState = MutableStateFlow(CustomerProfileUiState())
    val uiState: StateFlow<CustomerProfileUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CustomerProfileEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        initializeUserProfile()
        loadBusinessContactInfo()
    }

    /**
     * Initializes the user data flow.
     * 1. Checks Authentication status.
     * 2. Starts observing the Firestore User Profile stream.
     */
    private fun initializeUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Get Auth ID
            val authResult = getCurrentUserUseCase()
            if (authResult is Result.Success && authResult.data != null) {
                val userId = authResult.data.uid
                val email = authResult.data.email ?: ""

                // Set email immediately from Auth
                _uiState.update { it.copy(userEmail = email) }

                // 2. Start Listening to Firestore Stream
                observeUserProfile(userId)
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessageId = R.string.error_user_not_found)
                }
            }
        }
    }

    /**
     * Observes real-time updates from the 'users' collection.
     * If the user updates their name in the Edit screen, this flow triggers automatically.
     */
    private suspend fun observeUserProfile(userId: String) {
        getCustomerProfileUseCase(userId).collectLatest { result ->
            when (result) {
                is Result.Success -> {
                    val profile = result.data
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            // Use name from Firestore if available, otherwise keep existing or null
                            userName = profile.name?.takeIf { it.isNotBlank() },
                            // Update email if it exists in profile document, else keep Auth email
                            userEmail = if (!profile.email.isNullOrBlank()) profile.email else state.userEmail,
                            errorMessageId = null
                        )
                    }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Error streaming user profile")
                    // Do not block UI on stream error; Auth data is still valid.
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Loading -> {
                    // Stream is loading; usually fast enough to ignore explicit UI loading state here
                }
            }
        }
    }

    /**
     * Fetches the Business Profile (Storefront Info) using the target Owner ID.
     * This data is required for the "Business Contact" dialog.
     */
    private fun loadBusinessContactInfo() {
        viewModelScope.launch {
            val targetId = FirebaseConstants.TARGET_BUSINESS_OWNER_ID

            getBusinessProfileUseCase(targetId).collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(businessProfile = result.data) }
                }
            }
        }
    }

    /**
     * Signs out the user and triggers navigation to Login.
     */
    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _eventFlow.emit(CustomerProfileEvent.NavigateToLogin)
        }
    }
}