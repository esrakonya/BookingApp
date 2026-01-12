package com.stellarforge.composebooking.ui.screens.booking

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.CustomerProfile
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.usecase.CreateAppointmentUseCase
import com.stellarforge.composebooking.domain.usecase.GetAvailableSlotsUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetCustomerProfileUseCase
import com.stellarforge.composebooking.domain.usecase.GetServiceDetailsUseCase
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for [BookingViewModel].
 *
 * Focuses on the UI Logic and State Management of the booking flow:
 * - **Initialization:** Verifies that service details and initial slots are loaded.
 * - **User Input:** Tests validation logic for Customer Name and Phone.
 * - **State Updates:** Ensures UI state reflects Loading, Success, and Error states correctly.
 * - **Navigation:** Verifies that successful booking triggers the correct navigation event.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK private lateinit var mockGetAvailableSlotsUseCase: GetAvailableSlotsUseCase
    @RelaxedMockK private lateinit var mockGetServiceDetailsUseCase: GetServiceDetailsUseCase
    @RelaxedMockK private lateinit var mockCreateAppointmentUseCase: CreateAppointmentUseCase
    @RelaxedMockK private lateinit var mockGetCurrentUserUseCase: GetCurrentUserUseCase
    @RelaxedMockK private lateinit var mockGetCustomerProfileUseCase: GetCustomerProfileUseCase

    private lateinit var viewModel: BookingViewModel

    private val testUserId = "test-user-uid-123"
    private val testUser = AuthUser(uid = testUserId, email = "test@example.com")

    private val testCustomerProfile = CustomerProfile(name = "Pre-filled Name", phone = "5551234567")

    private val testServiceId = "service-xyz"
    private val testService = Service(
        id = testServiceId,
        ownerId = FirebaseConstants.TARGET_BUSINESS_OWNER_ID,
        name = "Test Service",
        durationMinutes = 45,
        priceInCents = 7500L
    )

    private val testDate = LocalDate.now()
    private val testSlots = listOf(LocalTime.of(10, 0), LocalTime.of(11, 0))
    private val testName = "Test Customer"
    private val testPhone = "5551234567"

    @Before
    fun setUp() {
        coEvery { mockGetServiceDetailsUseCase(testServiceId) } returns Result.Success(testService)

        every { mockGetAvailableSlotsUseCase(any(), any(), any()) } returns flowOf(Result.Success(testSlots))

        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(testUser)

        every { mockGetCustomerProfileUseCase(testUserId) } returns flowOf(Result.Success(testCustomerProfile))

        coEvery { mockCreateAppointmentUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.Success(Unit)
    }

    private fun createViewModel(serviceId: String? = testServiceId) {
        val savedStateHandle = SavedStateHandle(mapOf("serviceId" to serviceId))
        viewModel = BookingViewModel(
            savedStateHandle,
            mockGetAvailableSlotsUseCase,
            mockGetServiceDetailsUseCase,
            mockCreateAppointmentUseCase,
            mockGetCurrentUserUseCase,
            mockGetCustomerProfileUseCase
        )
    }

    @Test
    fun `init - with valid serviceId - loads service, slots AND prefills user info`() = runTest {
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        // Service & Slots Loaded
        assertThat(state.isLoadingService).isFalse()
        assertThat(state.service).isEqualTo(testService)
        assertThat(state.availableSlots).isEqualTo(testSlots)

        // Check Pre-fill Logic
        assertThat(state.customerName).isEqualTo(testCustomerProfile.name)
        assertThat(state.customerPhone).isEqualTo(testCustomerProfile.phone)
        assertThat(state.customerEmail).isEqualTo(testUser.email)

        assertThat(state.error).isNull()

        verify(exactly = 1) {
            mockGetAvailableSlotsUseCase(FirebaseConstants.TARGET_BUSINESS_OWNER_ID, testDate, testService.durationMinutes)
        }
    }

    @Test
    fun `init - with blank serviceId - sets error state`() = runTest {
        // ARRANGE
        createViewModel(serviceId = " ")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val state = viewModel.uiState.value
        assertThat(state.isLoadingService).isFalse()
        assertThat(state.service).isNull()
        assertThat(state.error).isEqualTo(R.string.error_service_id_missing)

        verify(exactly = 0) { mockGetAvailableSlotsUseCase(any(), any(), any()) }
    }

    @Test
    fun `init - when getServiceDetails fails - sets error state`() = runTest {
        // ARRANGE
        coEvery { mockGetServiceDetailsUseCase(testServiceId) } returns Result.Error(Exception("DB error"))

        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val state = viewModel.uiState.value
        assertThat(state.service).isNull()
        assertThat(state.error).isEqualTo(R.string.error_loading_service_details)

        verify(exactly = 0) { mockGetAvailableSlotsUseCase(any(), any(), any()) }
    }

    @Test
    fun `confirmBooking - with all valid data - calls createAppointmentUseCase with TARGET_ID`() = runTest {
        // ARRANGE
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSlotSelected(testSlots.first())
        viewModel.onCustomerNameChanged(testName)
        viewModel.onCustomerPhoneChanged(testPhone)

        // ACT
        viewModel.eventFlow.test {
            viewModel.confirmBooking()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(BookingViewEvent.NavigateToConfirmation)

            val finalState = viewModel.uiState.value
            assertThat(finalState.isBooking).isFalse()
            assertThat(finalState.bookingComplete).isTrue()
        }

        coVerify(exactly = 1) {
            mockCreateAppointmentUseCase(
                ownerId = FirebaseConstants.TARGET_BUSINESS_OWNER_ID,
                servicePriceInCents = testService.priceInCents,
                userId = testUser.uid,
                serviceId = testService.id,
                serviceName = testService.name,
                serviceDuration = testService.durationMinutes,
                date = testDate,
                time = testSlots.first(),
                customerName = testName,
                customerPhone = testPhone,
                customerEmail = any()
            )
        }
    }

    @Test
    fun `confirmBooking - when createAppointment fails - emits ShowSnackbar event`() = runTest {
        // ARRANGE
        coEvery { mockCreateAppointmentUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.Error(Exception("Create failed"))

        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSlotSelected(testSlots.first())
        viewModel.onCustomerNameChanged(testName)
        viewModel.onCustomerPhoneChanged(testPhone)

        // ACT & ASSERT
        viewModel.eventFlow.test {
            viewModel.confirmBooking()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertThat(event).isInstanceOf(BookingViewEvent.ShowSnackbar::class.java)
            assertThat((event as BookingViewEvent.ShowSnackbar).messageId).isEqualTo(R.string.error_booking_failed)
        }
    }

    @Test
    fun `confirmBooking - with invalid form data - does not call use case`() = runTest {
        // ARRANGE
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSlotSelected(testSlots.first())
        viewModel.onCustomerNameChanged(" ")
        viewModel.onCustomerPhoneChanged(testPhone)

        // ACT
        viewModel.confirmBooking()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        coVerify(exactly = 0) { mockCreateAppointmentUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }

        val state = viewModel.uiState.value
        assertThat(state.nameError).isEqualTo(R.string.error_name_empty)
    }
}