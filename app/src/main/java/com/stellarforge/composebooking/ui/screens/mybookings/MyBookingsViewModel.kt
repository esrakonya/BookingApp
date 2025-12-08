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

/**
 * Represents the UI state for the User's Booking List.
 *
 * @param bookings Holds the partitioned list (Upcoming vs Past).
 * @param isCancellingBookingId Holds the ID of the item currently being cancelled to show a spinner.
 */
data class MyBookingsUiState(
    val isLoading: Boolean = true,
    val bookings: MyBookings? = null,
    @StringRes val errorResId: Int? = null,
    val isCancellingBookingId: String? = null
)

sealed interface MyBookingsEvent {
    data class ShowSnackbar(@StringRes val messageId: Int): MyBookingsEvent
}

/**
 * ViewModel for the "My Bookings" screen.
 *
 * **Responsibilities:**
 * - **Data Loading:** Fetches the current user and subscribes to their appointment stream.
 * - **Real-time Updates:** Uses [GetMyBookingsUseCase] to separate bookings into 'Upcoming' and 'Past' categories via a Flow.
 * - **Cancellation:** Handles the logic for cancelling an appointment via [CancelBookingUseCase].
 * - **State Management:** Tracks loading states, errors, and specific item operations (e.g., cancelling spinner).
 */
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

    /**
     * Initiates the data loading process.
     * First verifies the user session, then starts listening to the booking stream.
     */
    fun loadMyBookings() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            // 1. Get Current User
            when (val userResult = getCurrentUserUseCase()) {
                is Result.Success -> {
                    val currentUser = userResult.data
                    if (currentUser != null && currentUser.uid.isNotBlank()) {
                        // 2. Start Listening to Stream
                        listenForBookings(currentUser.uid)
                    } else {
                        Timber.w("MyBookingsViewModel: Current user is null.")
                        _uiState.update {
                            it.copy(isLoading = false, errorResId = R.string.error_auth_user_not_found_for_services)
                        }
                    }
                }
                is Result.Error -> {
                    Timber.e(userResult.exception, "Error fetching user session.")
                    _uiState.update {
                        it.copy(isLoading = false, errorResId = R.string.error_user_not_found_generic)
                    }
                }
                is Result.Loading -> { }
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
                            errorResId = null
                        )
                    }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Error collecting bookings stream.")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorResId = R.string.error_loading_data_firestore
                        )
                    }
                }
                is Result.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Triggers the cancellation process for a specific appointment.
     *
     * @param bookingId The ID of the appointment to cancel.
     */
    fun cancelBooking(bookingId: String) {
        // Prevent duplicate requests or invalid calls
        if (bookingId.isBlank() || uiState.value.isCancellingBookingId != null) {
            return
        }

        viewModelScope.launch {
            // 1. Set specific item to loading state
            _uiState.update { it.copy(isCancellingBookingId = bookingId) }

            // 2. Call UseCase
            when (val result = cancelBookingUseCase(bookingId)) {
                is Result.Success -> {
                    Timber.i("Booking $bookingId cancelled successfully.")
                    _eventFlow.emit(MyBookingsEvent.ShowSnackbar(R.string.my_bookings_cancellation_success))
                    // The list will update automatically via the flow in listenForBookings
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to cancel booking $bookingId")
                    _eventFlow.emit(MyBookingsEvent.ShowSnackbar(R.string.my_bookings_cancellation_failed))
                }
                is Result.Loading -> {}
            }

            // 3. Clear loading state
            _uiState.update { it.copy(isCancellingBookingId = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorResId = null) }
    }
}