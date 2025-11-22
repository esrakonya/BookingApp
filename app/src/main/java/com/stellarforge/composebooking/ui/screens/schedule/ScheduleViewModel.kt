package com.stellarforge.composebooking.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val appointments: List<Appointment> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val getScheduleForDateUseCase: GetScheduleForDateUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private var currentOwnerId: String? = null

    init {
        loadInitialSchedule()
    }

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
                        _uiState.update { it.copy(isLoading = false, error = "İşletme sahibi oturumu bulunamadı.") }
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = userResult.message ?: "Kullanıcı bilgisi alınamadı.") }
                }
                is Result.Loading -> {  }
            }
        }
    }

    private fun loadScheduleForSelectedDate() {
        val ownerId = currentOwnerId
        if (ownerId == null) {
            _uiState.update { it.copy(error = "Randevuları getirmek için kullanıcı kimliği gerekli.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, appointments = emptyList()) }
            val date = _uiState.value.selectedDate

            when (val result = getScheduleForDateUseCase(ownerId, date)) {
                is Result.Success -> {
                    val sortedAppointments = result.data.sortedBy { it.appointmentDateTime }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            appointments = sortedAppointments,
                            error = null
                        )
                    }
                    Timber.d("Successfully loaded ${sortedAppointments.size} appointments for $date")
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Error loading schedule for $date")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Randevular yüklenemedi."
                        )
                    }
                }
                is Result.Loading -> {  }
            }
        }
    }

    /**
     * Kullanıcı takvimden yeni bir tarih seçtiğinde UI tarafından çağrılır.
     */
    fun onDateSelected(newDate: LocalDate) {
        if (newDate != _uiState.value.selectedDate) {
            _uiState.update { it.copy(selectedDate = newDate) }
            loadScheduleForSelectedDate()
        }
    }

    /**
     * Kullanıcı "Yeniden Dene" butonuna bastığında çağrılır.
     */
    fun onRetry() {
        if (currentOwnerId == null) {
            loadInitialSchedule()
        } else {
            loadScheduleForSelectedDate()
        }
    }
}