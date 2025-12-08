package com.stellarforge.composebooking.ui.screens.manageservices

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.usecase.DeleteServiceUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetOwnerServicesUseCase
import com.stellarforge.composebooking.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Represents the UI state for the Service Management screen.
 *
 * @param isDeletingServiceId Holds the ID of the service currently being deleted to show a loading spinner on that specific item.
 */
data class ManageServiceUiState(
    val isLoading: Boolean = true,
    val services: List<Service> = emptyList(),
    @StringRes val errorResId: Int? = null,
    val isDeletingServiceId: String? = null
)

/**
 * ViewModel for the "Manage Services" (Admin Dashboard) screen.
 *
 * **Responsibilities:**
 * - **Data Loading:** Fetches the list of services owned by the logged-in user (Active & Inactive).
 * - **State Management:** Handles loading states, error messages, and the list data.
 * - **Actions:** Executes business actions like deleting a service via [DeleteServiceUseCase].
 */
@HiltViewModel
class ManageServicesViewModel @Inject constructor(
    private val getOwnerServicesUseCase: GetOwnerServicesUseCase,
    private val deleteServiceUseCase: DeleteServiceUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageServiceUiState())
    val uiState: StateFlow<ManageServiceUiState> = _uiState.asStateFlow()

    init {
        loadServicesForOwner()
    }

    /**
     * Identifies the current owner and starts listening to their service stream.
     */
    fun loadServicesForOwner() {
        _uiState.update { it.copy(isLoading = true, errorResId = null) }

        viewModelScope.launch {
            when (val userResult = getCurrentUserUseCase()) {
                is Result.Success -> {
                    val currentUser = userResult.data
                    if (currentUser != null && currentUser.uid.isNotBlank()) {
                        listenForOwnerServices(currentUser.uid)
                    } else {
                        Timber.w("ManageServicesViewModel: User session not found.")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorResId = R.string.error_user_not_found_generic
                            )
                        }
                    }
                }
                is Result.Error -> {
                    Timber.e(userResult.exception, "ManageServicesViewModel: Auth check failed.")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorResId = R.string.error_user_not_found_generic
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    private suspend fun listenForOwnerServices(ownerId: String) {
        getOwnerServicesUseCase(ownerId).collectLatest { result ->
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, services = result.data, errorResId = null)
                    }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Error loading owner services stream.")
                    _uiState.update {
                        it.copy(isLoading = false, errorResId = R.string.error_services_loading_failed)
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Triggers the deletion of a specific service.
     * Shows a loading indicator on the specific item during the process.
     *
     * @param serviceId The unique ID of the service to delete.
     */
    fun deleteService(serviceId: String) {
        if (serviceId.isBlank()) return

        viewModelScope.launch {
            // 1. Set "Deleting" state for this specific item
            _uiState.update { it.copy(isDeletingServiceId = serviceId) }

            when (val result = deleteServiceUseCase(serviceId)) {
                is Result.Success -> {
                    Timber.i("Service deleted: $serviceId")
                    // Success! Clear the loading state.
                    // The list update will come automatically from the flow in listenForOwnerServices.
                    _uiState.update { it.copy(isDeletingServiceId = null) }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to delete service: $serviceId")
                    _uiState.update {
                        it.copy(
                            errorResId = R.string.error_service_deletion_failed,
                            isDeletingServiceId = null // Clear loading state on error too
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorResId = null) }
    }
}