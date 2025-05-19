package com.stellarforge.composebooking.ui.screens.booking

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.domain.usecase.CreateAppointmentUseCase
import com.stellarforge.composebooking.domain.usecase.GetAvailableSlotsUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetServiceDetailsUseCase
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
    val bookingResult: Result<Unit>? = null,
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val phoneErrorRes: Int? = null,
    @StringRes val slotsErrorMessage: Int? = null
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAvailableSlotsUseCase: GetAvailableSlotsUseCase,
    private val getServiceDetailsUseCase: GetServiceDetailsUseCase,
    private val createAppointmentUseCase: CreateAppointmentUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {
    private val serviceId: String = savedStateHandle.get<String>("serviceId") ?: ""
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<BookingViewEvent>(replay = 1)
    val eventFlow = _eventFlow.asSharedFlow()

    sealed interface BookingViewEvent {
        object NavigateToConfirmation : BookingViewEvent
        data class ShowSnackbar(@StringRes val messageId: Int, val formatArgs: List<Any> = emptyList()): BookingViewEvent
    }

    init {
        Timber.d("BookingViewModel initialized with serviceId: $serviceId")
        if (serviceId.isNotBlank()) {
            loadServiceDetails(serviceId)
        } else {
            Timber.w("Service ID is missing in BookingViewModel init.")
            viewModelScope.launch {
                _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_service_id_missing))
            }
            _uiState.update { it.copy(isLoadingService = false) }
        }
    }

    private fun loadServiceDetails(id: String) {
        _uiState.update { it.copy(isLoadingService = true) }
        viewModelScope.launch {
            val result = getServiceDetailsUseCase(id)
            if (result.isSuccess) {
                val service = result.getOrThrow()
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
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to load service details."
                Timber.e(result.exceptionOrNull(), "Error loading service details for serviceId: $id. Message: $errorMsg")
                _uiState.update { it.copy(isLoadingService = false) }
                _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_loading_service_details))
            }
        }
    }

    // Belirli bir tarih için müsait saatleri yükle
    private fun loadAvailableSlotsForDate(date: LocalDate) {
        viewModelScope.launch {
            // 1. Kullanıcı Kimlik Doğrulama Kontrolü
            Timber.d("Checking authentication before loading slots for date: $date")
            val userResult = getCurrentUserUseCase() // Mevcut kullanıcıyı al

            if (userResult.isFailure || userResult.getOrNull() == null) {
                // Kullanıcı giriş yapmamış veya hata var
                Timber.e(userResult.exceptionOrNull(), "User not authenticated when trying to load slots. Aborting slot loading.")
                _uiState.update {
                    it.copy(
                        isLoadingSlots = false, // Yükleniyor durumunu kapat
                        availableSlots = emptyList(), // Slot listesini temizle
                        slotsErrorMessage = R.string.error_auth_user_not_found // Hata mesajını ayarla (UiState'e eklenmeli)
                        // VEYA Snackbar göster:
                        // _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_auth_user_not_found))
                    )
                }
                // Opsiyonel: Login'e yönlendirme eventi
                // _eventFlow.emit(BookingViewEvent.NavigateToLogin)
                return@launch // Fonksiyondan çık
            }

            // Kullanıcı doğrulandıysa devam et
            val userId = userResult.getOrThrow()!!.uid // Kullanıcı null olamaz (yukarıda kontrol edildi)
            Timber.d("User $userId authenticated. Proceeding to load slots for date: $date")

            // 2. Servis Süresi Kontrolü
            val duration = uiState.value.serviceDuration
            if (duration <= 0) {
                Timber.w("Cannot load slots for user $userId, service duration is invalid or not available: $duration for date: $date")
                _uiState.update {
                    it.copy(
                        isLoadingSlots = false, // Yükleniyor durumunu kapat
                        availableSlots = emptyList(), // Slot listesini temizle
                        slotsErrorMessage = R.string.error_service_duration_not_available // Hata mesajını ayarla (UiState'e eklenmeli)
                    )
                }
                // Snackbar ile de gösterilebilir
                // _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_service_duration_not_available))
                return@launch // Fonksiyondan çık
            }

            // 3. Slot Yükleme İşlemini Başlat
            _uiState.update {
                it.copy(
                    isLoadingSlots = true, // Yükleniyor durumunu başlat
                    selectedDate = date,   // Seçilen tarihi güncelle
                    selectedSlot = null,   // Önceki seçili slotu temizle
                    availableSlots = emptyList(), // Önceki slotları temizle
                    slotsErrorMessage = null // Önceki slot hatalarını temizle
                )
            }

            // 4. UseCase'i Çağır ve Sonucu İşle
            getAvailableSlotsUseCase(date, duration)
                .catch { e ->
                    // Flow sırasında beklenmedik bir hata olursa
                    Timber.e(e, "Exception caught in getAvailableSlotsUseCase flow for date: $date, user: $userId")
                    _uiState.update {
                        it.copy(
                            isLoadingSlots = false,
                            slotsErrorMessage = R.string.error_loading_slots // Genel hata mesajı
                        )
                    }
                    // Snackbar ile de gösterilebilir
                    // _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_loading_slots))
                }
                .collect { result ->
                    if (result.isSuccess) {
                        val slots = result.getOrDefault(emptyList())
                        Timber.d("Successfully loaded ${slots.size} available slots for date: $date, user: $userId")
                        _uiState.update {
                            it.copy(
                                isLoadingSlots = false,
                                availableSlots = slots,
                                slotsErrorMessage = if (slots.isEmpty()) R.string.booking_screen_no_slots else null // Boşsa mesaj göster
                            )
                        }
                    } else {
                        // UseCase veya Repository katmanından Result.failure döndü
                        Timber.e(result.exceptionOrNull(), "Failed to load available slots (Result.failure) for date: $date, user: $userId")
                        _uiState.update {
                            it.copy(
                                isLoadingSlots = false,
                                availableSlots = emptyList(), // Hata durumunda listeyi boşalt
                                slotsErrorMessage = R.string.error_loading_slots // Genel hata mesajı
                            )
                        }
                        // Snackbar ile de gösterilebilir
                        // _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_loading_slots))
                    }
                }
        }
    }

    fun onDateSelected(newDate: LocalDate) {
        loadAvailableSlotsForDate(newDate)
    }

    fun onSlotSelected(slot: LocalTime) {
        _uiState.update { it.copy(selectedSlot = slot) }
    }

    fun onCustomerNameChanged(name: String) {
        _uiState.update { it.copy(customerName = name, nameErrorRes = null) }
    }

    fun onCustomerPhoneChanged(phone: String) {
        _uiState.update { it.copy(customerPhone = phone, phoneErrorRes = null) }
    }

    fun onCustomerEmailChanged(email: String) {
        _uiState.update { it.copy(customerEmail = email.takeIf { it.isNotBlank() }) }
    }

    private fun validateForm(state: BookingUiState): Boolean {
        var isValid = true
        var nameErrorId: Int? = null
        var phoneErrorId: Int? = null

        if (state.customerName.isBlank()) {
            nameErrorId = R.string.error_name_empty
            isValid = false
        }

        val minPhoneLength = 10 // Örnek minimum (Türkiye için alan kodu olmadan)
        val maxPhoneLength = 11

        if (state.customerPhone.isBlank()) {
            phoneErrorId = R.string.error_phone_empty
            isValid = false
        } else if (!state.customerPhone.all { it.isDigit() }) {
            phoneErrorId = R.string.error_phone_digits
            isValid = false
        } else if (state.customerPhone.length < minPhoneLength) {
            phoneErrorId = R.string.error_phone_too_short // Belki yeni bir string: error_phone_min_length
            isValid = false
        } else if (state.customerPhone.length > maxPhoneLength) {
            phoneErrorId = R.string.error_phone_too_long // Yeni string: error_phone_max_length
            isValid = false
        }

        // Hata mesajlarını state'e yansıt
        if (nameErrorId != state.nameErrorRes || phoneErrorId != state.phoneErrorRes) {
            _uiState.update { it.copy(nameErrorRes = nameErrorId, phoneErrorRes = phoneErrorId) }
        }

        return isValid
    }

    // Rezervasyonu onayla fonksiyonu (henüz işlevsel değil)
    fun confirmBooking() {
        val currentState = uiState.value

        if (!validateForm(currentState)) {
            return
        }

        if (currentState.selectedSlot == null) {
            viewModelScope.launch {
                _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_slot_missing))
            }
            return
        }

        if (currentState.isBooking || serviceId.isBlank() || currentState.serviceDuration <= 0) {
            Timber.w("Booking attemt blocked: isBooking=${currentState.isBooking}, serviceId=$serviceId, duration=${currentState.serviceDuration}")
            viewModelScope.launch {
                _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_booking_generic_problem))
            }
            return
        }

        _uiState.update { it.copy(isBooking = true, nameErrorRes = null, phoneErrorRes = null) }

        viewModelScope.launch {
            var userResult = getCurrentUserUseCase()

            if (userResult.isSuccess && userResult.getOrNull() != null) {
                val currentUser = userResult.getOrThrow()!!

                val userId = currentUser.uid

                Timber.d("User authenticated with UID: $userId. Proceeding with booking.")

                val createResult = createAppointmentUseCase(
                    userId = userId,
                    serviceId = serviceId,
                    serviceName = currentState.serviceName,
                    serviceDuration = currentState.serviceDuration,
                    date = currentState.selectedDate,
                    time = currentState.selectedSlot,
                    customerName = currentState.customerName,
                    customerEmail = currentState.customerEmail,
                    customerPhone = currentState.customerPhone
                )

                if (createResult.isSuccess) {
                    Timber.d("Booking Successful for service: ${currentState.serviceName}, customer: ${currentState.customerName}, userId: $userId")

                    _uiState.update { it.copy(isBooking = false, bookingResult = Result.success(Unit)) }

                    _eventFlow.emit(BookingViewEvent.NavigateToConfirmation)
                } else {
                    val errorMsg = createResult.exceptionOrNull()?.localizedMessage ?: "Could not complete booking."
                    Timber.e(createResult.exceptionOrNull(), "Booking Failed for service: ${currentState.serviceName}, customer: ${currentState.customerName}, userId: $userId. Message: $errorMsg")

                    _uiState.update {
                        it.copy(
                            isBooking = false,
                            bookingResult = Result.failure(createResult.exceptionOrNull() ?: Exception("Unknown booking error"))
                        )
                    }

                    _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_booking_failed))
                }
            } else {
                Timber.e(userResult.exceptionOrNull(), "Cannot create booking: User not found or error fetching user.")
                _uiState.update { it.copy(isBooking = false) }

                _eventFlow.emit(BookingViewEvent.ShowSnackbar(R.string.error_auth_user_not_found))
            }
        }
    }
}