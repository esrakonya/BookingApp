package com.stellarforge.composebooking.ui.screens.booking

import org.junit.Test
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.usecase.CreateAppointmentUseCase
import com.stellarforge.composebooking.domain.usecase.GetAvailableSlotsUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetServiceDetailsUseCase
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.seconds

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

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: BookingViewModel

    private val testUserId = "test-user-uid-123"
    private val testUser = AuthUser(uid = testUserId, email = "test@example.com")
    private val testServiceId = "service-xyz"
    private val testService = Service(id = testServiceId, name = "Detailed Service", durationMinutes = 45)
    private val testDate = LocalDate.now()
    private val testSlots = listOf(LocalTime.of(10, 0), LocalTime.of(11, 0))
    private val testName = "Test User"
    private val testPhone = "1234567890"
    private val testEmail = "test@example.com"

    @Before
    fun setUp() {
        // Varsayılan başarılı dönüşler
        coEvery { mockGetServiceDetailsUseCase(any()) } returns Result.Success(testService)
        every { mockGetAvailableSlotsUseCase(any(), any()) } returns flowOf(Result.Success(testSlots))
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(testUser)
        coEvery { mockCreateAppointmentUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.Success(Unit)
    }

    private fun initializeViewModel() {
        if (!::savedStateHandle.isInitialized) {
            savedStateHandle = SavedStateHandle(mapOf("serviceId" to testServiceId))
        }
        viewModel = BookingViewModel(
            savedStateHandle, mockGetAvailableSlotsUseCase, mockGetServiceDetailsUseCase,
            mockCreateAppointmentUseCase, mockGetCurrentUserUseCase
        )
    }


    @Test
    fun `onScreenReady - with valid serviceId - loads data successfully`() = runTest {
        initializeViewModel()

        viewModel.onScreenReady()
        advanceUntilIdle() // onScreenReady içindeki tüm coroutine'lerin bitmesini bekle

        val finalState = viewModel.uiState.value
        assertFalse("isLoadingService should be false", finalState.isLoadingService)
        assertEquals(testService.name, finalState.serviceName)
        assertEquals(testSlots, finalState.availableSlots)

        coVerify(exactly = 1) { mockGetServiceDetailsUseCase(testServiceId) }
    }

    @Test
    fun `onScreenReady - with blank serviceId - emits ShowSnackbar event`() = runTest {
        savedStateHandle = SavedStateHandle(mapOf("serviceId" to " "))
        initializeViewModel()

        viewModel.eventFlow.test(timeout = 1.seconds) {
            viewModel.onScreenReady() // Eylemi test bloğu içinde tetikle
            val event = awaitItem()
            assertTrue(event is BookingViewEvent.ShowSnackbar)
            assertEquals(R.string.error_service_id_missing, (event as BookingViewEvent.ShowSnackbar).messageId)
        }
    }

    @Test
    fun `onScreenReady - when getServiceDetails fails - emits ShowSnackbar event`() = runTest {
        coEvery { mockGetServiceDetailsUseCase(testServiceId) } returns Result.Error(Exception("Service error"))
        initializeViewModel()

        viewModel.eventFlow.test(timeout = 1.seconds) {
            viewModel.onScreenReady()
            val event = awaitItem()
            assertTrue(event is BookingViewEvent.ShowSnackbar)
            assertEquals(R.string.error_loading_service_details, (event as BookingViewEvent.ShowSnackbar).messageId)
        }
    }

    // --- confirmBooking() Testleri ---

    @Test
    fun `confirmBooking - with valid data - emits navigation event`() = runTest {
        initializeViewModel()
        viewModel.onScreenReady()
        advanceUntilIdle()

        viewModel.onSlotSelected(testSlots.first())
        viewModel.onCustomerNameChanged(testName)
        viewModel.onCustomerPhoneChanged(testPhone)

        viewModel.eventFlow.test(timeout = 1.seconds) {
            viewModel.confirmBooking()
            assertEquals(BookingViewEvent.NavigateToConfirmation, awaitItem())
        }
    }

    @Test
    fun `confirmBooking - when createAppointmentUseCase fails - sends snackbar event`() = runTest {
        coEvery { mockCreateAppointmentUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.Error(Exception("Create error"))
        initializeViewModel()
        viewModel.onScreenReady()
        advanceUntilIdle()

        viewModel.onSlotSelected(testSlots.first())
        viewModel.onCustomerNameChanged(testName)
        viewModel.onCustomerPhoneChanged(testPhone)

        viewModel.eventFlow.test(timeout = 1.seconds) {
            viewModel.confirmBooking()
            val event = awaitItem()
            assertEquals(R.string.error_booking_failed, (event as BookingViewEvent.ShowSnackbar).messageId)
        }
    }
}