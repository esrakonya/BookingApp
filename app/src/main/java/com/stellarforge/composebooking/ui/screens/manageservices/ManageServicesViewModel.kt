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

data class ManageServiceUiState(
    val isLoading: Boolean = true,
    val services: List<Service> = emptyList(),
    @StringRes val errorResId: Int? = null,
    val isDeletingServiceId: String? = null
)

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

    fun loadServicesForOwner() {
        _uiState.update { it.copy(isLoading = true, errorResId = null) }

        viewModelScope.launch {
            when (val userResult = getCurrentUserUseCase()) {
                is Result.Success -> {
                    val currentUser = userResult.data
                    if (currentUser != null && currentUser.uid.isNotBlank()) {
                        listenForOwnerServices(currentUser.uid)
                    } else {
                        // DÜZELTME: String yerine R.string ID'si kullan
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorResId = R.string.error_user_not_found_generic
                            )
                        }
                        Timber.w("Current user could not be determined for loading services.")
                    }
                }

                is Result.Error -> {
                    // DÜZELTME: String yerine R.string ID'si kullan
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorResId = R.string.error_user_not_found_generic
                        )
                    }
                    Timber.e(userResult.exception, "Error fetching current user.")
                }

                is Result.Loading -> {}
            }

        }
    }

    private suspend fun listenForOwnerServices(ownerId: String) {
        getOwnerServicesUseCase(ownerId).collectLatest { result ->
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, services = result.data, errorResId = null) }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Error loading owner services.")
                    // DÜZELTME: String yerine R.string ID'si kullan
                    _uiState.update { it.copy(isLoading = false, errorResId = R.string.error_services_loading_failed) }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Kullanıcı bir servisi silmek için butona bastığında çağrılır.
     * @param serviceId Silinecek servisin ID'si.
     */
    fun deleteService(serviceId: String) {
        if (serviceId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingServiceId = serviceId) }
            when (val result = deleteServiceUseCase(serviceId)) {
                is Result.Success -> {
                    Timber.i("Service with ID $serviceId marked for deletion.")
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to delete service with ID $serviceId.")
                    // DÜZELTME: String yerine R.string ID'si kullan
                    _uiState.update { it.copy(errorResId = R.string.error_service_deletion_failed) }
                }
                is Result.Loading -> {}
            }
            _uiState.update { it.copy(isDeletingServiceId = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorResId = null) }
    }

}