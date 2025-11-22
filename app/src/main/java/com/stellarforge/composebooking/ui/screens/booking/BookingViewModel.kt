package com.stellarforge.composebooking.ui.screens.booking

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.usecase.CreateAppointmentUseCase
import com.stellarforge.composebooking.domain.usecase.GetAvailableSlotsUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetServiceDetailsUseCase
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

// --- YENİ VE TEMİZ UI STATE ---
// Hata alanları birleştirildi. Gereksiz alanlar kaldırıldı.
data class BookingUiState(
    val isLoadingService: Boolean = true,
    val service: Service? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val availableSlots: List<LocalTime> = emptyList(),
    val isLoadingSlots: Boolean = false,
    val selectedSlot: LocalTime? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val isBooking: Boolean = false,
    val bookingComplete: Boolean = false,
    @StringRes val error: Int? = null, // Tüm genel hatalar için
    @StringRes val nameError: Int? = null, // Sadece isim alanı için
    @StringRes val phoneError: Int? = null // Sadece telefon alanı için
)

sealed interface BookingViewEvent {
    object NavigateToConfirmation : BookingViewEvent
    data class ShowSnackbar(@StringRes val messageId: Int) : BookingViewEvent
}

@HiltViewModel
class BookingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAvailableSlotsUseCase: GetAvailableSlotsUseCase,
    private val getServiceDetailsUseCase: GetServiceDetailsUseCase,
    private val createAppointmentUseCase: CreateAppointmentUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {
    private val serviceId: String = savedStateHandle.get<String>("serviceId")!!

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<BookingViewEvent>()
    val eventFlow: SharedFlow<BookingViewEvent> = _eventFlow.asSharedFlow()

    init {
        Timber.d("BookingViewModel initialized with serviceId: $serviceId")
        loadServiceDetails()
    }

    private fun loadServiceDetails() {
        if (serviceId.isBlank()) {
            _uiState.update { it.copy(isLoadingService = false, error = R.string.error_service_id_missing) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingService = true) }
            when (val result = getServiceDetailsUseCase(serviceId)) {
                is Result.Success -> {
                    val service = result.data
                    _uiState.update { it.copy(service = service) }
                    if (service != null) {
                        _uiState.update { it.copy(service = service) }
                        loadAvailableSlotsForDate(uiState.value.selectedDate)
                    } else {
                        _uiState.update { it.copy(isLoadingService = false, error = R.string.error_service_not_found) }
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingService = false, error = R.string.error_loading_service_details) }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun loadAvailableSlotsForDate(date: LocalDate) {
        val serviceDuration = uiState.value.service?.durationMinutes ?: return
        _uiState.update { it.copy(isLoadingSlots = true, availableSlots = emptyList(), selectedSlot = null) }
        viewModelScope.launch {
            val ownerId = FirebaseConstants.TARGET_BUSINESS_OWNER_ID
            getAvailableSlotsUseCase(ownerId, date, serviceDuration)
                .collect { result ->
                    _uiState.update {
                        when (result) {
                            is Result.Success -> it.copy(
                                isLoadingService = false,
                                isLoadingSlots = false,
                                availableSlots = result.data,
                                error = if (result.data.isEmpty()) R.string.booking_screen_no_slots else null
                            )
                            is Result.Error -> it.copy(isLoadingService = false, isLoadingSlots = false, error = R.string.error_loading_slots)
                            is Result.Loading -> it.copy(isLoadingSlots = true)
                        }
                    }
                }
        }
    }

    fun onDateSelected(newDate: LocalDate) {
        _uiState.update { it.copy(selectedDate = newDate) }
        loadAvailableSlotsForDate(newDate)
    }

    fun onSlotSelected(slot: LocalTime) {
        _uiState.update { it.copy(selectedSlot = slot) }
    }

    fun onCustomerNameChanged(name: String) {
        _uiState.update { it.copy(customerName = name, nameError = null) }
    }

    fun onCustomerPhoneChanged(phone: String) {
        _uiState.update { it.copy(customerPhone = phone, phoneError = null) }
    }

    fun onCustomerEmailChanged(email: String) {
        _uiState.update { it.copy(customerEmail = email) }
    }

    // --- DÜZELTİLMİŞ validateForm FONKSİYONU ---
    private fun validateForm(): Boolean {
        val state = uiState.value
        val nameError = if (state.customerName.isBlank()) R.string.error_name_empty else null
        val phoneError = when {
            state.customerPhone.isBlank() -> R.string.error_phone_empty
            state.customerPhone.length < 10 -> R.string.error_phone_length // Örnek validasyon
            else -> null
        }

        _uiState.update { it.copy(nameError = nameError, phoneError = phoneError) }

        return nameError == null && phoneError == null
    }

    // --- DÜZELTİLMİŞ confirmBooking FONKSİYONU ---
    fun confirmBooking() {
        if (!validateForm()) return

        val currentState = uiState.value
        val service = currentState.service
        val selectedSlot = currentState.selectedSlot

        if (service == null || selectedSlot == null) {
            viewModelScope.launch { _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_booking_generic_problem)) }
            return
        }

        _uiState.update { it.copy(isBooking = true) }

        viewModelScope.launch {
            val userResult = getCurrentUserUseCase()
            val currentUser = (userResult as? Result.Success)?.data

            if (currentUser == null) {
                _uiState.update { it.copy(isBooking = false) }
                _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_auth_user_not_found))
                return@launch
            }
            val result = createAppointmentUseCase(
                ownerId = FirebaseConstants.TARGET_BUSINESS_OWNER_ID,
                servicePriceInCents = service.priceInCents,
                userId = currentUser.uid,
                serviceId = service.id,
                serviceName = service.name,
                serviceDuration = service.durationMinutes,
                date = currentState.selectedDate,
                time = selectedSlot,
                customerName = currentState.customerName,
                customerPhone = currentState.customerPhone,
                customerEmail = currentState.customerEmail.takeIf { it.isNotBlank() }
            )

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isBooking = false, bookingComplete = true) }
                    _eventFlow.emit(BookingViewEvent.NavigateToConfirmation)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isBooking = false) }
                    _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_booking_failed))
                }
                is Result.Loading -> {}
            }
        }
    }
}