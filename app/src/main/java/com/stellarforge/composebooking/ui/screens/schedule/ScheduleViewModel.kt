package com.stellarforge.composebooking.ui.screens.schedule

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetScheduleForDateUseCase
import com.stellarforge.composebooking.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

/**
 * Represents the UI state for the Schedule (Calendar) screen.
 *
 * @param selectedDate The date currently selected in the horizontal calendar.
 * @param appointments The list of bookings for the [selectedDate].
 * @param errorResId Localized error message resource ID, if any operation fails.
 */
data class ScheduleUiState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val appointments: List<Appointment> = emptyList(),
    @StringRes val errorResId: Int? = null
)

/**
 * ViewModel for the **Owner Schedule** (Dashboard) screen.
 *
 * **Responsibilities:**
 * - **Authentication Check:** Verifies the current user is an Owner and retrieves their UID.
 * - **Data Fetching:** Retrieves appointments for a specific date via [GetScheduleForDateUseCase].
 * - **State Management:** Handles date selection, loading states, and error propagation.
 *
 * This is the central hub for the Business Owner to view their daily operations.
 */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val getScheduleForDateUseCase: GetScheduleForDateUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    // Caches the owner's UID to prevent re-fetching auth on every date change.
    private var currentOwnerId: String? = null

    init {
        loadInitialSchedule()
    }

    /**
     * First-time initialization.
     * Checks if the user is logged in, gets the UID, and loads today's schedule.
     */
    private fun loadInitialSchedule() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val userResult = getCurrentUserUseCase()) {
                is Result.Success -> {
                    val currentUser = userResult.data
                    if (currentUser != null && currentUser.uid.isNotBlank()) {
                        currentOwnerId = currentUser.uid
                        loadScheduleForSelectedDate()
                    } else {
                        Timber.w("ScheduleViewModel: User not authenticated.")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorResId = R.string.error_user_not_found_generic
                            )
                        }
                    }
                }
                is Result.Error -> {
                    Timber.e(userResult.exception, "ScheduleViewModel: Auth check failed.")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorResId = R.string.error_user_not_found_generic
                        )
                    }
                }
                is Result.Loading -> { }
            }
        }
    }

    /**
     * Fetches appointments for the currently selected date using the cached [currentOwnerId].
     */
    private fun loadScheduleForSelectedDate() {
        val ownerId = currentOwnerId
        if (ownerId == null) {
            _uiState.update { it.copy(errorResId = R.string.error_user_not_found_generic) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, appointments = emptyList()) }
            val date = _uiState.value.selectedDate

            Timber.d("Loading schedule for date: $date")

            when (val result = getScheduleForDateUseCase(ownerId, date)) {
                is Result.Success -> {
                    // Sort appointments by time for better UX
                    val sortedAppointments = result.data.sortedBy { it.appointmentDateTime }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            appointments = sortedAppointments,
                            errorResId = null
                        )
                    }
                    Timber.d("Loaded ${sortedAppointments.size} appointments.")
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Error loading schedule.")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorResId = R.string.error_loading_data_firestore
                        )
                    }
                }
                is Result.Loading -> { }
            }
        }
    }

    /**
     * Called when the user taps a different day in the Calendar.
     * Updates the state and triggers a data reload.
     */
    fun onDateSelected(newDate: LocalDate) {
        if (newDate != _uiState.value.selectedDate) {
            _uiState.update { it.copy(selectedDate = newDate) }
            loadScheduleForSelectedDate()
        }
    }

    /**
     * Handles the "Retry" button click on error screens.
     * Smartly decides whether to retry Authentication or just the Data Load.
     */
    fun onRetry() {
        if (currentOwnerId == null) {
            loadInitialSchedule()
        } else {
            loadScheduleForSelectedDate()
        }
    }
}