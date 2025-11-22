package com.stellarforge.composebooking.ui.screens.mybookings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.domain.usecase.CancelBookingUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetMyBookingsUseCase
import com.stellarforge.composebooking.domain.usecase.MyBookings
import com.stellarforge.composebooking.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class MyBookingsUiState(
    val isLoading: Boolean = true,
    val bookings: MyBookings? = null,
    val error: String? = null,
    val isCancellingBookingId: String? = null
)

sealed interface MyBookingsEvent {
    data class ShowSnackbar(@StringRes val messageId: Int): MyBookingsEvent
}

@HiltViewModel
class MyBookingsViewModel @Inject constructor(
    private val getMyBookingsUseCase: GetMyBookingsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val cancelBookingUseCase: CancelBookingUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyBookingsUiState())
    val uiState: StateFlow<MyBookingsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<MyBookingsEvent>()
    val eventFlow: SharedFlow<MyBookingsEvent> = _eventFlow.asSharedFlow()

    init {
        loadMyBookings()
    }

    fun loadMyBookings() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            when (val userResult = getCurrentUserUseCase()) {
                is Result.Success -> {
                    val currentUser = userResult.data
                    if (currentUser != null && currentUser.uid.isNotBlank()) {
                        listenForBookings(currentUser.uid)
                    } else {
                        Timber.w("Current user is null, cannot load bookings.")
                        _uiState.update { it.copy(isLoading = false, error = "Randevularınızı getirmek için lütfen giriş yapın.") }
                    }
                }
                is Result.Error -> {
                    Timber.e(userResult.exception, "Error fetching current user for bookings.")
                    _uiState.update { it.copy(isLoading = false, error = userResult.message ?: "Kullanıcı bilgisi alınamadı.") }
                }
                is Result.Loading -> {  }
            }
        }
    }

    private suspend fun listenForBookings(userId: String) {
        getMyBookingsUseCase(userId).collectLatest { result ->
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            bookings = result.data,
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Error collecting bookings.")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Randevular yüklenemedi."
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun cancelBooking(bookingId: String) {
        if (bookingId.isBlank() || uiState.value.isCancellingBookingId != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCancellingBookingId = bookingId) }

            when (val result = cancelBookingUseCase(bookingId)) {
                is Result.Success -> {
                    Timber.i("Booking with ID $bookingId cancelled successfully.")
                    _eventFlow.emit(MyBookingsEvent.ShowSnackbar(R.string.my_bookings_cancellation_success))
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to cancel booking with ID $bookingId")
                    _eventFlow.emit(MyBookingsEvent.ShowSnackbar(R.string.my_bookings_cancellation_failed))
                }
                is Result.Loading -> {}
            }

            _uiState.update { it.copy(isCancellingBookingId = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}