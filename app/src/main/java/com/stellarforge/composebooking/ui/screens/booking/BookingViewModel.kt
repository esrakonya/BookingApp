package com.stellarforge.composebooking.ui.screens.booking

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// import androidx.lifecycle.viewmodel.compose.viewModel // Bu import genellikle Composable içinde kullanılır, ViewModel'da değil
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser // AuthUser importu
import com.stellarforge.composebooking.data.model.Service // Service importu
import com.stellarforge.composebooking.domain.usecase.CreateAppointmentUseCase
import com.stellarforge.composebooking.domain.usecase.GetAvailableSlotsUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetServiceDetailsUseCase
import com.stellarforge.composebooking.utils.Result // KENDİ Result.kt dosyanın importu
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class BookingUiState(
    val isLoadingService: Boolean = false,
    val serviceName: String = "",
    val serviceDuration: Int = 0,
    val selectedDate: LocalDate = LocalDate.now(),
    val availableSlots: List<LocalTime> = emptyList(),
    val isLoadingSlots: Boolean = false,
    val selectedSlot: LocalTime? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String? = null,
    val isBooking: Boolean = false,
    val bookingSuccess: Boolean? = null, // Başarı/hata için ayrı flag'ler veya mesajlar
    @StringRes val bookingMessageRes: Int? = null,
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val phoneErrorRes: Int? = null,
    @StringRes val slotsErrorMessage: Int? = null,
    @StringRes val serviceLoadErrorMessage: Int? = null // Servis yükleme hatası için
)

sealed interface BookingViewEvent {
    object NavigateToConfirmation : BookingViewEvent
    data class ShowSnackbar(@StringRes val messageId: Int, val formatArgs: List<Any> = emptyList()) : BookingViewEvent
}

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getAvailableSlotsUseCase: GetAvailableSlotsUseCase,
    private val getServiceDetailsUseCase: GetServiceDetailsUseCase, // Bunun Result<Service?> döndürdüğünü varsayıyoruz
    private val createAppointmentUseCase: CreateAppointmentUseCase, // Bunun Result<Unit> döndürdüğünü varsayıyoruz
    private val getCurrentUserUseCase: GetCurrentUserUseCase // Bunun Result<AuthUser?> döndürdüğünü varsayıyoruz
) : ViewModel() {
    private val serviceId: String = savedStateHandle.get<String>("serviceId") ?: ""
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<BookingViewEvent>() // replay=1 kaldırıldı, event'ler tek seferlik olmalı
    val eventFlow: SharedFlow<BookingViewEvent> = _eventFlow.asSharedFlow()


    init {
        Timber.d("BookingViewModel initialized with serviceId: $serviceId")
    }

    fun onScreenReady() {
        if (uiState.value.serviceName.isNotBlank() || uiState.value.isLoadingService) return

        _uiState.update { it.copy(isLoadingService = true) }
        if (serviceId.isNotBlank()) {
            loadServiceDetails(serviceId)
        } else {
            Timber.w("Service ID is missing.")
            _uiState.update { it.copy(isLoadingService = false, serviceLoadErrorMessage = R.string.error_service_id_missing) }
            viewModelScope.launch {
                _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_service_id_missing))
            }
        }
    }

    private fun loadServiceDetails(id: String) {
        viewModelScope.launch {
            when (val result = getServiceDetailsUseCase(id)) {
                is Result.Success -> {
                    val service = result.data
                    if (service != null) {
                        _uiState.update {
                            it.copy(
                                serviceName = service.name,
                                serviceDuration = service.durationMinutes
                            )
                        }
                        loadAvailableSlotsForDate(uiState.value.selectedDate)
                    } else {
                        _uiState.update { it.copy(isLoadingService = false, serviceLoadErrorMessage = R.string.error_service_not_found) }
                        _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_service_not_found))
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingService = false, serviceLoadErrorMessage = R.string.error_loading_service_details) }
                    _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_loading_service_details))
                }
                is Result.Loading -> {}
            }
        }
    }


    private fun loadAvailableSlotsForDate(date: LocalDate) {
        _uiState.update { it.copy(isLoadingSlots = true, selectedDate = date, selectedSlot = null, availableSlots = emptyList()) }
        viewModelScope.launch {
            getAvailableSlotsUseCase(date, uiState.value.serviceDuration)
                .collect { slotResult ->
                    when (slotResult) {
                        is Result.Success -> {
                            val slots = slotResult.data
                            _uiState.update {
                                it.copy(
                                    isLoadingService = false,
                                    isLoadingSlots = false,
                                    availableSlots = slots,
                                    slotsErrorMessage = if (slots.isEmpty()) R.string.booking_screen_no_slots else null
                                )
                            }
                        }
                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoadingService = false,
                                    isLoadingSlots = false,
                                    slotsErrorMessage = R.string.error_loading_slots
                                )
                            }
                        }
                        is Result.Loading -> {
                            _uiState.update { it.copy(isLoadingSlots = true) }
                        }
                    }
                }
        }
    }

    fun onDateSelected(newDate: LocalDate) {
        _uiState.update { it.copy(selectedSlot = null) } // Tarih değiştiğinde seçili slotu temizle
        loadAvailableSlotsForDate(newDate)
    }

    fun onSlotSelected(slot: LocalTime) {
        _uiState.update { it.copy(selectedSlot = slot) }
    }

    fun onCustomerNameChanged(name: String) {
        _uiState.update { it.copy(customerName = name, nameErrorRes = null, bookingMessageRes = null) }
    }

    fun onCustomerPhoneChanged(phone: String) {
        _uiState.update { it.copy(customerPhone = phone, phoneErrorRes = null, bookingMessageRes = null) }
    }

    fun onCustomerEmailChanged(email: String) {
        _uiState.update { it.copy(customerEmail = email.takeIf { it.isNotBlank() }, bookingMessageRes = null) }
    }

    private fun validateForm(): Boolean { // Güncel: state'i doğrudan uiState.value'dan alacak
        val currentState = uiState.value // Fonksiyon içinde güncel state'i alalım
        var isValid = true

        // İsim Validasyonu
        val newNameError: Int? = if (currentState.customerName.isBlank()) {
            isValid = false
            R.string.error_name_empty
        } else {
            null
        }

        // Telefon Numarası Validasyonu
        val newPhoneError: Int?
        when {
            currentState.customerPhone.isBlank() -> {
                newPhoneError = R.string.error_phone_empty
                isValid = false
            }
            !currentState.customerPhone.all { it.isDigit() } -> {
                // Telefon numarası sadece rakamlardan oluşmuyorsa
                newPhoneError = R.string.error_phone_digits
                isValid = false
            }
            // Türkiye için örnek 10 (örn. 5xxxxxxxxx) veya 11 (örn. 05xxxxxxxxx) hane kontrolü.
            // Bu kuralı kendi ihtiyaçlarınıza göre (ülke, format vb.) değiştirebilirsiniz.
            currentState.customerPhone.length !in 10..11 -> {
                newPhoneError = R.string.error_phone_length
                isValid = false
            }
            else -> {
                newPhoneError = null
            }
        }

        // E-posta Validasyonu (Opsiyonel, eğer customerEmail zorunluysa veya girildiyse kontrol edilebilir)
        // Şu anki BookingUiState'te customerEmail nullable ve zorunlu değil gibi duruyor.
        // Eğer girilmişse ve validasyon yapmak isterseniz:
        /*
        val newEmailError: Int? = currentState.customerEmail?.let { email ->
            if (email.isNotBlank() && !ValidationUtils.isEmailValid(email)) { // ValidationUtils'taki isEmailValid kullanılır
                isValid = false
                R.string.error_email_invalid_format // strings.xml'e eklenmeli
            } else {
                null
            }
        }
        */
        // Hata mesajlarını state'e yansıt (sadece gerçekten bir değişiklik olduysa)
        // BookingUiState'e emailErrorRes de eklemeniz gerekir eğer e-posta validasyonu yapacaksanız.
        if (newNameError != currentState.nameErrorRes || newPhoneError != currentState.phoneErrorRes /* || newEmailError != currentState.emailErrorRes */) {
            _uiState.update {
                it.copy(
                    nameErrorRes = newNameError,
                    phoneErrorRes = newPhoneError
                    // emailErrorRes = newEmailError // Eğer e-posta validasyonu eklerseniz
                )
            }
        }

        return isValid
    }


    fun confirmBooking() {
        if (!validateForm()) {
            Timber.d("confirmBooking: Form validation failed.")
            return
        }

        val currentState = uiState.value
        if (currentState.selectedSlot == null) {
            Timber.w("confirmBooking: Slot not selected.")
            viewModelScope.launch { _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_slot_missing)) }
            return
        }

        if (currentState.isBooking) {
            Timber.d("confirmBooking: Booking already in progress.")
            return
        }

        _uiState.update { it.copy(isBooking = true) }

        viewModelScope.launch {
            when (val userResult = getCurrentUserUseCase()) {
                is Result.Success -> {
                    val currentUser = userResult.data
                    if (currentUser == null || currentUser.uid.isBlank()) {
                        Timber.e("confirmBooking: User not authenticated.")
                        _uiState.update { it.copy(isBooking = false) }
                        _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_auth_user_not_found))
                        return@launch
                    }

                    // --- TAMAMLANMIŞ KISIM ---
                    when (val createResult = createAppointmentUseCase(
                        userId = currentUser.uid,
                        serviceId = this@BookingViewModel.serviceId, // Sınıf seviyesindeki serviceId
                        serviceName = currentState.serviceName,
                        serviceDuration = currentState.serviceDuration,
                        date = currentState.selectedDate,
                        time = currentState.selectedSlot, // Null olamaz (yukarıda kontrol edildi)
                        customerName = currentState.customerName,
                        customerPhone = currentState.customerPhone,
                        customerEmail = currentState.customerEmail
                    )) {
                        // --- TAMAMLANMIŞ KISIM SONU ---
                        is Result.Success -> {
                            Timber.i("Booking Successful for user: ${currentUser.uid}")
                            _uiState.update { it.copy(isBooking = false, bookingSuccess = true, bookingMessageRes = R.string.booking_success) }
                            _eventFlow.emit(BookingViewEvent.NavigateToConfirmation)
                        }
                        is Result.Error -> {
                            Timber.e(createResult.exception, "Booking Failed.")
                            _uiState.update { it.copy(isBooking = false, bookingSuccess = false, bookingMessageRes = R.string.error_booking_failed) }
                            _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_booking_failed))
                        }
                        is Result.Loading -> { /* Bu case beklenmez */ }
                    }
                }
                is Result.Error -> {
                    Timber.e(userResult.exception, "confirmBooking: Error fetching user.")
                    _uiState.update { it.copy(isBooking = false, bookingSuccess = false, bookingMessageRes = R.string.error_auth_user_not_found) }
                    _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_auth_user_not_found))
                }
                is Result.Loading -> { /* Bu case beklenmez */ }
            }
        }
    }

}