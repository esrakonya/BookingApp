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
    val isLoadingService: Boolean = true,
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

@HiltViewModel
class BookingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAvailableSlotsUseCase: GetAvailableSlotsUseCase,
    private val getServiceDetailsUseCase: GetServiceDetailsUseCase, // Bunun Result<Service?> döndürdüğünü varsayıyoruz
    private val createAppointmentUseCase: CreateAppointmentUseCase, // Bunun Result<Unit> döndürdüğünü varsayıyoruz
    private val getCurrentUserUseCase: GetCurrentUserUseCase // Bunun Result<AuthUser?> döndürdüğünü varsayıyoruz
) : ViewModel() {
    private val serviceId: String = savedStateHandle.get<String>("serviceId") ?: ""
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    // BookingViewEvent'i global (dosya seviyesinde) veya ViewModel içinde tanımlayabilirsin
    sealed interface BookingViewEvent {
        object NavigateToConfirmation : BookingViewEvent
        data class ShowSnackbar(@StringRes val messageId: Int, val formatArgs: List<Any> = emptyList()) : BookingViewEvent
        // object NavigateToLogin : BookingViewEvent // Eğer gerekirse
    }

    private val _eventFlow = MutableSharedFlow<BookingViewEvent>() // replay=1 kaldırıldı, event'ler tek seferlik olmalı
    val eventFlow: SharedFlow<BookingViewEvent> = _eventFlow.asSharedFlow()


    init {
        Timber.d("BookingViewModel initialized with serviceId: $serviceId")
        if (serviceId.isNotBlank()) {
            loadServiceDetails(serviceId)
        } else {
            Timber.w("Service ID is missing in BookingViewModel init.")
            _uiState.update { it.copy(isLoadingService = false, serviceLoadErrorMessage = R.string.error_service_id_missing) }
            // Snackbar ile de gösterilebilir
            // viewModelScope.launch {
            //     _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_service_id_missing))
            // }
        }
    }

    private fun loadServiceDetails(id: String) {
        _uiState.update { it.copy(isLoadingService = true, serviceLoadErrorMessage = null) }
        viewModelScope.launch {
            // GetServiceDetailsUseCase'in Result<Service?> döndürdüğünü varsayıyoruz
            when (val result = getServiceDetailsUseCase(id)) {
                is Result.Success -> {
                    val service = result.data // Service? tipinde
                    if (service != null) {
                        Timber.d("ViewModel: Service details loaded: ${service.name}")
                        _uiState.update {
                            it.copy(
                                isLoadingService = false,
                                serviceName = service.name,
                                serviceDuration = service.durationMinutes
                            )
                        }
                        loadAvailableSlotsForDate(uiState.value.selectedDate)
                    } else {
                        Timber.e("Service details loaded as null for serviceId: $id.")
                        _uiState.update { it.copy(isLoadingService = false, serviceLoadErrorMessage = R.string.error_service_not_found) }
                    }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Error loading service details for serviceId: $id. Message: ${result.message}")
                    _uiState.update { it.copy(isLoadingService = false, serviceLoadErrorMessage = R.string.error_loading_service_details) }
                }
                is Result.Loading -> {
                    // UseCase suspend fun ise bu beklenmez.
                    _uiState.update { it.copy(isLoadingService = true) }
                }
            }
        }
    }

    private fun loadAvailableSlotsForDate(date: LocalDate) {
        viewModelScope.launch {
            Timber.d("Checking authentication before loading slots for date: $date")
            // GetCurrentUserUseCase'in Result<AuthUser?> döndürdüğünü varsayıyoruz
            when (val userResult = getCurrentUserUseCase()) {
                is Result.Success -> {
                    val authUser = userResult.data // AuthUser? tipinde
                    if (authUser == null || authUser.uid.isBlank()) {
                        Timber.e("User not authenticated or UID blank when trying to load slots. Aborting.")
                        _uiState.update {
                            it.copy(
                                isLoadingSlots = false,
                                availableSlots = emptyList(),
                                slotsErrorMessage = R.string.error_auth_user_not_found
                            )
                        }
                        return@launch
                    }

                    val userId = authUser.uid
                    Timber.d("User $userId authenticated. Proceeding to load slots for date: $date")

                    val duration = uiState.value.serviceDuration
                    if (duration <= 0) {
                        Timber.w("Cannot load slots for user $userId, service duration is invalid: $duration for date: $date")
                        _uiState.update {
                            it.copy(
                                isLoadingSlots = false,
                                availableSlots = emptyList(),
                                slotsErrorMessage = R.string.error_service_duration_not_available
                            )
                        }
                        return@launch
                    }

                    _uiState.update {
                        it.copy(
                            isLoadingSlots = true,
                            selectedDate = date,
                            selectedSlot = null,
                            availableSlots = emptyList(),
                            slotsErrorMessage = null
                        )
                    }

                    // GetAvailableSlotsUseCase'in Flow<Result<List<LocalTime>>> döndürdüğünü varsayıyoruz
                    getAvailableSlotsUseCase(date, duration)
                        .catch { e ->
                            Timber.e(e, "Exception caught in getAvailableSlotsUseCase flow for date: $date, user: $userId")
                            _uiState.update {
                                it.copy(isLoadingSlots = false, slotsErrorMessage = R.string.error_loading_slots)
                            }
                        }
                        .collect { slotResult -> // slotResult tipi Result<List<LocalTime>>
                            when (slotResult) {
                                is Result.Success -> {
                                    val slots = slotResult.data // List<LocalTime> tipinde
                                    Timber.d("Successfully loaded ${slots.size} available slots for date: $date, user: $userId")
                                    _uiState.update {
                                        it.copy(
                                            isLoadingSlots = false,
                                            availableSlots = slots,
                                            slotsErrorMessage = if (slots.isEmpty()) R.string.booking_screen_no_slots else null
                                        )
                                    }
                                }
                                is Result.Error -> {
                                    Timber.e(slotResult.exception, "Failed to load available slots (Result.Error) for date: $date, user: $userId. Message: ${slotResult.message}")
                                    _uiState.update {
                                        it.copy(
                                            isLoadingSlots = false,
                                            availableSlots = emptyList(),
                                            slotsErrorMessage = R.string.error_loading_slots
                                        )
                                    }
                                }
                                is Result.Loading -> {
                                    // Bu, Flow'dan gelen bir Loading durumu olabilir, UI'ı güncelleyebiliriz.
                                    Timber.d("GetAvailableSlotsUseCase flow emitted Loading for date: $date, user: $userId")
                                    _uiState.update { it.copy(isLoadingSlots = true) }
                                }
                            }
                        }
                }
                is Result.Error -> {
                    Timber.e(userResult.exception, "Failed to get current user. Aborting slot loading. Message: ${userResult.message}")
                    _uiState.update {
                        it.copy(isLoadingSlots = false, slotsErrorMessage = R.string.error_auth_user_not_found)
                    }
                }
                is Result.Loading -> {
                    Timber.d("GetCurrentUserUseCase returned Loading. Waiting for user info.")
                    // İsteğe bağlı: Kullanıcı bilgisi yüklenirken bir UI güncellemesi
                    // _uiState.update { it.copy(isLoadingSlots = true, slotsErrorMessage = R.string.loading_user_info) }
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
        val currentState = uiState.value

        if (!validateForm()) return

        if (currentState.selectedSlot == null) {
            viewModelScope.launch { _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_slot_missing)) }
            return
        }

        if (currentState.isBooking || serviceId.isBlank() || currentState.serviceDuration <= 0) {
            Timber.w("Booking attempt blocked: isBooking=${currentState.isBooking}, serviceId=$serviceId, duration=${currentState.serviceDuration}")
            viewModelScope.launch { _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_booking_generic_problem)) }
            return
        }

        _uiState.update { it.copy(isBooking = true, nameErrorRes = null, phoneErrorRes = null, bookingMessageRes = null) }

        viewModelScope.launch {
            when (val userResult = getCurrentUserUseCase()) {
                is Result.Success -> {
                    val currentUser = userResult.data
                    if (currentUser == null || currentUser.uid.isBlank()) {
                        Timber.e("Cannot create booking: User not found or UID blank after re-check.")
                        _uiState.update { it.copy(isBooking = false, bookingMessageRes = R.string.error_auth_user_not_found) }
                        return@launch
                    }
                    val userId = currentUser.uid
                    Timber.d("User $userId authenticated. Proceeding with booking.")

                    // CreateAppointmentUseCase'in Result<Unit> döndürdüğünü varsayıyoruz
                    when (val createResult = createAppointmentUseCase(
                        userId = userId,
                        serviceId = serviceId,
                        serviceName = currentState.serviceName,
                        serviceDuration = currentState.serviceDuration,
                        date = currentState.selectedDate,
                        time = currentState.selectedSlot, // Null olamaz (yukarıda kontrol edildi)
                        customerName = currentState.customerName,
                        customerEmail = currentState.customerEmail,
                        customerPhone = currentState.customerPhone
                    )) {
                        is Result.Success -> {
                            Timber.d("Booking Successful for service: ${currentState.serviceName}, customer: ${currentState.customerName}, userId: $userId")
                            _uiState.update { it.copy(isBooking = false, bookingSuccess = true, bookingMessageRes = R.string.booking_success) }
                            _eventFlow.emit(BookingViewEvent.NavigateToConfirmation)
                        }
                        is Result.Error -> {
                            Timber.e(createResult.exception, "Booking Failed for service: ${currentState.serviceName}. Message: ${createResult.message}")
                            _uiState.update {
                                it.copy(isBooking = false, bookingSuccess = false, bookingMessageRes = R.string.error_booking_failed)
                            }
                        }
                        is Result.Loading -> {
                            // Bu case'in çalışması beklenmez
                        }
                    }
                }
                is Result.Error -> {
                    Timber.e(userResult.exception, "Cannot create booking: Error fetching user. Message: ${userResult.message}")
                    _uiState.update { it.copy(isBooking = false, bookingMessageRes = R.string.error_auth_user_not_found) }
                }
                is Result.Loading -> {
                    Timber.d("GetCurrentUserUseCase is Loading during booking. Retrying or failing.")
                    _uiState.update { it.copy(isBooking = false, bookingMessageRes = R.string.error_auth_user_not_found) } // Veya farklı bir mesaj
                }
            }
        }
    }
}