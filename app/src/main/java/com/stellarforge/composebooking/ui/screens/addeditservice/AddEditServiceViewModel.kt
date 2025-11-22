package com.stellarforge.composebooking.ui.screens.addeditservice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

data class AddEditServiceUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val serviceSaved: Boolean = false, // Kayıt başarılı olduğunda true olur ve bir önceki ekrana dönülür
    val screenTitle: String = "Yeni Servis Ekle",
    val error: String? = null,

    // Form Alanları
    val name: String = "",
    val description: String = "",
    val duration: String = "", // Kullanıcıdan String olarak alıp sonra Int'e çevireceğiz
    val price: String = "",    // Kullanıcıdan String olarak alıp sonra Long'a (kuruş) çevireceğiz
    val isActive: Boolean = true
)

@HiltViewModel
class AddEditServiceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getServiceDetailsUseCase: GetServiceDetailsUseCase,
    private val addServiceUseCase: AddServiceUseCase,
    private val updateServiceUseCase: UpdateServiceUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {
    private val serviceId: String? =savedStateHandle.get("serviceId")

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
                                screenTitle = "Servisi Düzenle",
                                name = service.name,
                                description = service.description,
                                duration = service.durationMinutes.toString(),
                                // Kuruşları (Long) tekrar TL String'ine çeviriyoruz (örn: 15050 -> "150.50")
                                price = (service.priceInCents / 100.0).toString(),
                                isActive = service.isActive
                            )
                        }
                    } ?: _uiState.update { it.copy(isLoading = false, error = "Servis bulunamadı.") }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message ?: "Servis yüklenemedi.") }
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
            val userResult = getCurrentUserUseCase()
            val currentUser = (userResult as? Result.Success)?.data
            if (currentUser == null) {
                _uiState.update { it.copy(error = "İşlem için kullanıcı doğrulaması gerekli.") }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true) }

            val currentState = _uiState.value
            val durationInt = currentState.duration.toIntOrNull()
            // Fiyatı kuruşa çeviriyoruz (örn: "150.5" -> 15050L)
            val priceInCents = currentState.price.replace(',', '.').toDoubleOrNull()?.let { (it * 100).toLong() }

            // Form Validasyonu
            if (currentState.name.isBlank() || durationInt == null || priceInCents == null) {
                _uiState.update { it.copy(isSaving = false, error = "Lütfen tüm zorunlu alanları doğru doldurun.") }
                return@launch
            }

            val serviceToSave = (originalService ?: Service()).copy(
                ownerId = currentUser.uid,
                name = currentState.name.trim(),
                description = currentState.description.trim(),
                durationMinutes = durationInt,
                priceInCents = priceInCents,
                isActive = currentState.isActive
            )

            val result = if (serviceId == null) {
                // Ekleme Modu
                addServiceUseCase(serviceToSave)
            } else {
                // Düzenleme Modu
                updateServiceUseCase(serviceToSave)
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, serviceSaved = true) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message ?: "Servis kaydedilemedi.") }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}