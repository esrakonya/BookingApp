package com.stellarforge.composebooking.ui.screens.servicelist

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.domain.usecase.GetBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// --- Events & State ---

sealed interface ServiceListViewEvent {
    object NavigateToLogin : ServiceListViewEvent
    data class ShowSnackbar(@StringRes val messageResId: Int) : ServiceListViewEvent
}

sealed interface ServiceListUiState {
    object Loading : ServiceListUiState
    data class Success(val services: List<Service>) : ServiceListUiState
    data class Error(
        @StringRes val messageResId: Int,
        val exception: Exception? = null
    ) : ServiceListUiState
}

/**
 * ViewModel for the **Customer Home Screen** (Storefront).
 *
 * **Responsibilities:**
 * - **Data Loading:** Fetches the list of active services (Products) available for booking.
 * - **Branding:** Loads the Business Profile (Name) to display on the top bar.
 * - **Auth Check:** Verifies if the user is authenticated before showing data.
 * - **Session Management:** Handles the Sign-Out process.
 */
@HiltViewModel
class ServiceListViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository, // Kept for future booking features
    private val serviceRepository: ServiceRepository,
    private val signOutUseCase: SignOutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getBusinessProfileUseCase: GetBusinessProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ServiceListUiState>(ServiceListUiState.Loading)
    val uiState: StateFlow<ServiceListUiState> = _uiState.asStateFlow()

    private val _businessProfile = MutableStateFlow<BusinessProfile?>(null)
    val businessProfile: StateFlow<BusinessProfile?> = _businessProfile.asStateFlow()

    // Holds the Business Name to be displayed in the TopBar
    private val _businessName = MutableStateFlow<String?>(null)
    val businessName: StateFlow<String?> = _businessName.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ServiceListViewEvent>()
    val eventFlow: SharedFlow<ServiceListViewEvent> = _eventFlow.asSharedFlow()

    init {
        Timber.d("ServiceListViewModel initialized.")
        loadInitialData()
    }

    /**
     * Orchestrates the initial data loading process.
     * 1. Starts loading the Business Profile (Independent of user auth).
     * 2. Checks User Auth status.
     * 3. If authenticated, loads the Service Catalog.
     */
    fun loadInitialData() {
        _uiState.value = ServiceListUiState.Loading
        _businessName.value = null
        Timber.d("ServiceListViewModel: Loading initial data...")

        viewModelScope.launch {
            // Task A: Load Shop Name (Always visible)
            launch {
                loadBusinessProfile(FirebaseConstants.TARGET_BUSINESS_OWNER_ID)
            }

            // Task B: Check User & Load Services
            when (val userResult = getCurrentUserUseCase()) {
                is Result.Success -> {
                    val authUser = userResult.data
                    if (authUser != null && authUser.uid.isNotBlank()) {
                        Timber.d("User authenticated: ${authUser.uid}")
                        loadServices()
                    } else {
                        handleUserAuthError("User session is invalid.")
                    }
                }
                is Result.Error -> {
                    handleUserAuthError("Auth check failed.", userResult.exception)
                }
                is Result.Loading -> { }
            }
        }
    }

    private suspend fun loadBusinessProfile(targetOwnerId: String) {
        getBusinessProfileUseCase(targetOwnerId).collect { result ->
            if (result is Result.Success) {
               _businessProfile.value = result.data
            } else if (result is Result.Error) {
                Timber.e("Failed to load business profile. Default title will be used.")
            }
        }
    }

    private suspend fun loadServices() {
        Timber.d("Loading customer service catalog...")

        // Fetch ACTIVE services only (Customer Stream)
        serviceRepository.getCustomerServicesStream()
            .retryWhen { cause, attempt ->
                // Retry logic for transient network errors
                if (attempt < 1 && cause is FirebaseFirestoreException) {
                    delay(300)
                    return@retryWhen true
                }
                return@retryWhen false
            }
            .catch { e ->
                Timber.e(e, "Error in service stream")
                _uiState.value = ServiceListUiState.Error(R.string.error_loading_data)
            }
            .collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.value = ServiceListUiState.Success(result.data)
                    }
                    is Result.Error -> {
                        val errorRes = determineFirestoreErrorMessage(result.exception as? Exception)
                        _uiState.value = ServiceListUiState.Error(errorRes, result.exception as? Exception)
                    }
                    is Result.Loading -> {
                        _uiState.value = ServiceListUiState.Loading
                    }
                }
            }
    }

    // --- Helper Functions for Error Handling ---

    private fun handleUserAuthError(logMessage: String, exception: Exception? = null) {
        Timber.e(exception, "ServiceListViewModel: $logMessage")
        val errorRes = determineAuthErrorMessage(exception)
        _uiState.value = ServiceListUiState.Error(errorRes, exception)
    }

    private fun determineFirestoreErrorMessage(exception: Exception?): Int {
        return when (exception) {
            is FirebaseFirestoreException -> when (exception.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_auth_permission_denied_services
                FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_network_connection_firestore
                else -> R.string.error_loading_data_firestore
            }
            else -> R.string.error_loading_data
        }
    }

    private fun determineAuthErrorMessage(exception: Exception?): Int {
        return when (exception) {
            is FirebaseAuthException -> R.string.error_auth_generic
            is FirebaseNetworkException -> R.string.error_network_connection
            else -> R.string.error_auth_user_not_found_for_services
        }
    }

    // --- User Actions ---

    fun onRetryClicked() {
        loadInitialData()
    }

    fun signOut() {
        viewModelScope.launch {
            when (val result = signOutUseCase()) {
                is Result.Success<*> -> {
                    _eventFlow.emit(ServiceListViewEvent.NavigateToLogin)
                }
                is Result.Error -> {
                    _eventFlow.emit(ServiceListViewEvent.ShowSnackbar(R.string.error_sign_out_failed))
                }
                is Result.Loading -> { }
            }
        }
    }
}