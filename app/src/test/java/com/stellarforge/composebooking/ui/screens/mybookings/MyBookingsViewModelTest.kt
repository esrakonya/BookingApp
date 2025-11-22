package com.stellarforge.composebooking.ui.screens.mybookings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.R
import com.google.firebase.Timestamp
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.domain.usecase.CancelBookingUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetMyBookingsUseCase
import com.stellarforge.composebooking.domain.usecase.MyBookings
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

@ExperimentalCoroutinesApi
class MyBookingsViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @RelaxedMockK private lateinit var getMyBookingsUseCase: GetMyBookingsUseCase
    @RelaxedMockK private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase
    @RelaxedMockK private lateinit var cancelBookingUseCase: CancelBookingUseCase

    private lateinit var viewModel: MyBookingsViewModel

    private val testUser = AuthUser(uid = "test-user-id")
    private val upcomingBooking = Appointment(
        id = "upcoming-1",
        serviceName = "Upcoming",
        appointmentDateTime = Timestamp(Date(System.currentTimeMillis() + 100000))
    )
    private val pastBooking = Appointment(
        id = "past-1",
        serviceName = "Past",
        appointmentDateTime = Timestamp(Date(System.currentTimeMillis() + 100000))
    )
    private val testBookings = MyBookings(
        upcomingBookings = listOf(upcomingBooking),
        pastBookings = listOf(pastBooking)
    )

    @Before
    fun setUp() {
        coEvery { getCurrentUserUseCase() } returns Result.Success(testUser)
        every { getMyBookingsUseCase(any()) } returns flowOf(Result.Success(testBookings))
        coEvery { cancelBookingUseCase(any()) } returns Result.Success(Unit)
    }

    private fun createViewModel() {
        viewModel = MyBookingsViewModel(
            getMyBookingsUseCase,
            getCurrentUserUseCase,
            cancelBookingUseCase
        )
    }

    @Test
    fun `init - when user is authenticated - loads bookings successfully`() = runTest {
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.bookings).isEqualTo(testBookings)
        assertThat(state.error).isNull()

        coVerify(exactly = 1) { getCurrentUserUseCase() }
        verify(exactly = 1) { getMyBookingsUseCase(testUser.uid) }
    }

    @Test
    fun `init - when user is not authenticated - sets error state`() = runTest {
        coEvery { getCurrentUserUseCase() } returns Result.Success(null)

        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.bookings).isNull()
        assertThat(state.error).isEqualTo("Randevularınızı getirmek için lütfen giriş yapın.")

        verify(exactly = 0) { getMyBookingsUseCase(any()) }
    }

    @Test
    fun `cancelBooking - when successful - calls use case and shows success snackbar`() = runTest {
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.cancelBooking("upcoming-1")
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()

            assertThat(event).isInstanceOf(MyBookingsEvent.ShowSnackbar::class.java)
            assertThat((event as MyBookingsEvent.ShowSnackbar).messageId).isEqualTo(R.string.my_bookings_cancellation_success)

            val state = viewModel.uiState.value
            assertThat(state.isCancellingBookingId).isNull()
        }

        coVerify(exactly = 1) { cancelBookingUseCase("upcoming-1") }
    }

    @Test
    fun `cancelBooking - when use case fails - shows failed snackbar`() = runTest {
        coEvery { cancelBookingUseCase(any()) } returns Result.Error(Exception("Deletion failed"))

        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eventFlow.test {
            viewModel.cancelBooking("upcoming-1")
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()

            assertThat(event).isInstanceOf(MyBookingsEvent.ShowSnackbar::class.java)
            assertThat((event as MyBookingsEvent.ShowSnackbar).messageId).isEqualTo(R.string.my_bookings_cancellation_failed)

            val state = viewModel.uiState.value
            assertThat(state.isCancellingBookingId).isNull()
        }

        coVerify(exactly = 1) { cancelBookingUseCase("upcoming-1") }
    }
}