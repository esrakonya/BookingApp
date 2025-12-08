package com.stellarforge.composebooking.ui.screens.addeditservice

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.usecase.AddServiceUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetServiceDetailsUseCase
import com.stellarforge.composebooking.domain.usecase.UpdateServiceUseCase
import com.stellarforge.composebooking.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

data class AddEditServiceUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val serviceSaved: Boolean = false,

    @StringRes val screenTitle: Int = R.string.add_edit_service_screen_title_add,
    @StringRes val error: Int? = null,

    // Form Fields
    val name: String = "",
    val description: String = "",
    val duration: String = "",
    val price: String = "",
    val isActive: Boolean = true
)

/**
 * ViewModel responsible for the "Add New Service" and "Edit Service" screens.
 *
 * **Responsibilities:**
 * - **Mode Detection:** Determines if the screen is in 'Add' or 'Edit' mode based on the `serviceId` navigation argument.
 * - **Data Loading:** Fetches existing service details if in 'Edit' mode.
 * - **Input Validation:** Validates Name, Duration, and Price (handling BigDecimal conversion).
 * - **Persistance:** Calls the appropriate UseCase ([AddServiceUseCase] or [UpdateServiceUseCase]) to save changes.
 */
@HiltViewModel
class AddEditServiceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getServiceDetailsUseCase: GetServiceDetailsUseCase,
    private val addServiceUseCase: AddServiceUseCase,
    private val updateServiceUseCase: UpdateServiceUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val serviceId: String? = savedStateHandle.get("serviceId")

    private val _uiState = MutableStateFlow(AddEditServiceUiState())
    val uiState: StateFlow<AddEditServiceUiState> = _uiState.asStateFlow()

    private var originalService: Service? = null

    init {
        if (serviceId != null) {
            loadServiceDetails(serviceId)
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadServiceDetails(id: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = getServiceDetailsUseCase(id)) {
                is Result.Success -> {
                    result.data?.let { service ->
                        originalService = service
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                screenTitle = R.string.add_edit_service_screen_title_edit,
                                name = service.name,
                                description = service.description,
                                duration = service.durationMinutes.toString(),
                                // Convert cents back to formatted string (e.g., 15050 -> "150.5")
                                price = (service.priceInCents / 100.0).toString(),
                                isActive = service.isActive
                            )
                        }
                    } ?: _uiState.update {
                        it.copy(isLoading = false, error = R.string.error_service_not_found)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = R.string.error_loading_service_details)
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun onNameChange(newName: String) { _uiState.update { it.copy(name = newName) } }
    fun onDescriptionChange(newDescription: String) { _uiState.update { it.copy(description = newDescription) } }
    fun onDurationChange(newDuration: String) { _uiState.update { it.copy(duration = newDuration.filter { it.isDigit() }) } }
    fun onPriceChange(newPrice: String) { _uiState.update { it.copy(price = newPrice) } }
    fun onIsActiveChange(newIsActive: Boolean) { _uiState.update { it.copy(isActive = newIsActive) } }

    fun saveService() {
        viewModelScope.launch {
            val currentState = _uiState.value

            // Log in English for maintainability
            Timber.tag("AddEditService").d("Attempting to save service: Name='${currentState.name}', Duration='${currentState.duration}', Price='${currentState.price}'")

            val userResult = getCurrentUserUseCase()
            val currentUser = (userResult as? Result.Success)?.data
            if (currentUser == null) {
                _uiState.update { it.copy(error = R.string.error_auth_user_not_found) }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, error = null) }

            // 1. Validate Name
            if (currentState.name.isBlank()) {
                _uiState.update { it.copy(isSaving = false, error = R.string.error_name_empty) }
                return@launch
            }

            // 2. Validate Duration
            val durationInt = currentState.duration.toIntOrNull()
            if (durationInt == null || durationInt <= 0) {
                _uiState.update { it.copy(isSaving = false, error = R.string.error_booking_generic_problem) }
                return@launch
            }

            // 3. Validate Price (BigDecimal)
            val priceInCents = try {
                val cleanPrice = currentState.price.replace(',', '.').trim()
                if (cleanPrice.isEmpty()) throw NumberFormatException()

                BigDecimal(cleanPrice).movePointRight(2).toLong()
            } catch (e: Exception) {
                Timber.e(e, "Price conversion error for input: ${currentState.price}")
                null
            }

            if (priceInCents == null || priceInCents < 0) {
                _uiState.update { it.copy(isSaving = false, error = R.string.error_booking_generic_problem) }
                return@launch
            }

            // 4. Prepare Object
            val serviceToSave = (originalService ?: Service()).copy(
                ownerId = currentUser.uid,
                name = currentState.name.trim(),
                description = currentState.description.trim(),
                durationMinutes = durationInt,
                priceInCents = priceInCents,
                isActive = currentState.isActive
            )

            // 5. Execute UseCase
            val result = if (serviceId == null) {
                addServiceUseCase(serviceToSave)
            } else {
                updateServiceUseCase(serviceToSave)
            }

            when (result) {
                is Result.Success -> {
                    Timber.tag("AddEditService").d("Service saved successfully.")
                    _uiState.update { it.copy(isSaving = false, serviceSaved = true) }
                }
                is Result.Error -> {
                    Timber.tag("AddEditService").e(result.exception, "Save failed: ${result.message}")
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = R.string.error_booking_failed
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}