package com.stellarforge.composebooking.ui.screens.schedule

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetScheduleForDateUseCase
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.R
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@ExperimentalCoroutinesApi
class ScheduleViewModelTest {

    @get:Rule
    val mockKRule = MockKRule(this)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @RelaxedMockK private lateinit var getScheduleForDateUseCase: GetScheduleForDateUseCase
    @RelaxedMockK private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    private lateinit var viewModel: ScheduleViewModel

    private val testUser = AuthUser(uid = "test-owner-id")
    private val today = LocalDate.now()
    private val tomorrow = today.plusDays(1)
    private val todaysAppointments = listOf(Appointment(id = "appt-1", serviceName = "Appointment for Today"))
    private val tomorrowsAppointments = listOf(Appointment(id = "appt-2", serviceName = "Appointment for Tomorrow"))

    @Before
    fun setUp() {
        coEvery { getCurrentUserUseCase() } returns Result.Success(testUser)
        coEvery { getScheduleForDateUseCase(testUser.uid, today) } returns Result.Success(todaysAppointments)
        coEvery { getScheduleForDateUseCase(testUser.uid, tomorrow) } returns Result.Success(tomorrowsAppointments)
    }

    private fun createViewModel() {
        viewModel = ScheduleViewModel(getScheduleForDateUseCase, getCurrentUserUseCase)
    }

    @Test
    fun `init - when user is authenticated - loads schedule for today successfully`() = runTest {
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.selectedDate).isEqualTo(today)
        assertThat(state.appointments).isEqualTo(todaysAppointments)
        assertThat(state.errorResId).isNull()

        coVerify(exactly = 1) { getCurrentUserUseCase() }
        coVerify(exactly = 1) { getScheduleForDateUseCase(testUser.uid, today) }
    }

    @Test
    fun `init - when getCurrentUser fails - sets error state`() = runTest {
        val exception = Exception("Auth error")

        // Mock failure
        coEvery { getCurrentUserUseCase() } returns Result.Error(exception)

        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorResId).isNotNull()
        assertThat(state.errorResId).isEqualTo(R.string.error_user_not_found_generic)
        assertThat(state.appointments).isEmpty()

        coVerify(exactly = 0) { getScheduleForDateUseCase(any(), any()) }
    }

    @Test
    fun `onDateSelected - with a new date - loads schedule for the new date`() = runTest {
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.appointments).isEqualTo(todaysAppointments)

        // ACT: Change date
        viewModel.onDateSelected(tomorrow)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.selectedDate).isEqualTo(tomorrow)
        assertThat(state.appointments).isEqualTo(tomorrowsAppointments)

        coVerify(exactly = 1) { getScheduleForDateUseCase(testUser.uid, tomorrow) }
    }

    @Test
    fun `onDateSelected - with the same date - does not reload data`() = runTest {
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ACT: Select same date
        viewModel.onDateSelected(today)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Should be called only once (during init)
        coVerify(exactly = 1) { getScheduleForDateUseCase(testUser.uid, today) }
    }

    @Test
    fun `onRetry - when user was not loaded - re-runs initial load`() = runTest {
        // Init fails first
        coEvery { getCurrentUserUseCase() } returns Result.Error(Exception("Auth error"))
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Fix mock
        coEvery { getCurrentUserUseCase() } returns Result.Success(testUser)

        // Retry
        viewModel.onRetry()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.appointments).isEqualTo(todaysAppointments)

        coVerify(exactly = 2) { getCurrentUserUseCase() }
    }
}