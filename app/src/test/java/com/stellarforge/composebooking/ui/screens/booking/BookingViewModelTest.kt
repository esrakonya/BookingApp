package com.stellarforge.composebooking.ui.screens.booking

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
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockGetAvailableSlotsUseCase: GetAvailableSlotsUseCase
    private lateinit var mockGetServiceDetailsUseCase: GetServiceDetailsUseCase
    private lateinit var mockCreateAppointmentUseCase: CreateAppointmentUseCase
    private lateinit var mockGetCurrentUserUseCase: GetCurrentUserUseCase
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
        mockGetAvailableSlotsUseCase = mockk(relaxed = true)
        mockGetServiceDetailsUseCase = mockk(relaxed = true)
        mockCreateAppointmentUseCase = mockk(relaxed = true)
        mockGetCurrentUserUseCase = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("serviceId" to testServiceId))

        coEvery { mockGetServiceDetailsUseCase(testServiceId) } returns Result.success(testService)
        every { mockGetAvailableSlotsUseCase(any(), any()) } returns flowOf(Result.success(testSlots))
        coEvery { mockGetCurrentUserUseCase() } returns Result.success(testUser) // Varsayılan olarak kullanıcı dönsün
        coEvery {
            mockCreateAppointmentUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(Unit)
    }

    private fun initializeViewModel() {
        viewModel = BookingViewModel(
            savedStateHandle,
            mockGetAvailableSlotsUseCase,
            mockGetServiceDetailsUseCase,
            mockCreateAppointmentUseCase,
            mockGetCurrentUserUseCase
        )
    }

    @Test
    fun `init successfully loads service details and initial slots calling getCurrentUser once`() = runTest {
        val specificSlotsResult = Result.success(testSlots)
        every { mockGetAvailableSlotsUseCase(testDate, testService.durationMinutes) } returns flowOf(specificSlotsResult)

        initializeViewModel()
        advanceUntilIdle()

        viewModel.uiState.test(timeout = 3.seconds) {
            val finalState = expectMostRecentItem()
            assertFalse("Service loading should be false", finalState.isLoadingService)
            assertEquals(testService.name, finalState.serviceName)
            assertEquals(testService.durationMinutes, finalState.serviceDuration)
            assertEquals(testSlots, finalState.availableSlots)
        }
        coVerify(exactly = 1) { mockGetServiceDetailsUseCase(testServiceId) }
        verify(exactly = 1) { mockGetAvailableSlotsUseCase(testDate, testService.durationMinutes) }
        // loadAvailableSlotsForDate, init'te servis yüklendikten sonra çağrılır ve getCurrentUser'ı çağırır.
        coVerify(exactly = 1) { mockGetCurrentUserUseCase() }
        coVerify(exactly = 0) { mockCreateAppointmentUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `init with blank serviceId emits ShowSnackbar and does not load data`() = runTest {
        savedStateHandle = SavedStateHandle(mapOf("serviceId" to ""))
        initializeViewModel()
        advanceUntilIdle()

        viewModel.eventFlow.test(timeout = 3.seconds) {
            val event = awaitItem()
            assertTrue(event is BookingViewModel.BookingViewEvent.ShowSnackbar)
            assertEquals(R.string.error_service_id_missing, (event as BookingViewModel.BookingViewEvent.ShowSnackbar).messageId)
            expectNoEvents()
        }
        coVerify(exactly = 0) { mockGetServiceDetailsUseCase(any()) }
        verify(exactly = 0) { mockGetAvailableSlotsUseCase(any(), any()) }
        coVerify(exactly = 0) { mockGetCurrentUserUseCase() }
    }

    @Test
    fun `init fails to load service details emits ShowSnackbar and does not load slots`() = runTest {
        coEvery { mockGetServiceDetailsUseCase(testServiceId) } returns Result.failure(Exception("Service error"))
        initializeViewModel()
        advanceUntilIdle()

        viewModel.eventFlow.test(timeout = 3.seconds) {
            val event = awaitItem()
            assertTrue(event is BookingViewModel.BookingViewEvent.ShowSnackbar)
            assertEquals(R.string.error_loading_service_details, (event as BookingViewModel.BookingViewEvent.ShowSnackbar).messageId)
            expectNoEvents()
        }
        verify(exactly = 0) { mockGetAvailableSlotsUseCase(any(), any()) }
        coVerify(exactly = 0) { mockGetCurrentUserUseCase() } // Slot yüklenmeyeceği için bu da çağrılmaz
    }

    @Test
    fun `onDateSelected updates date and loads new slots calling getCurrentUser`() = runTest {
        initializeViewModel()
        advanceUntilIdle() // init'in tamamlanmasını bekle

        val newDate = testDate.plusDays(1)
        val newSlots = listOf(LocalTime.of(14, 0))
        every { mockGetAvailableSlotsUseCase(newDate, testService.durationMinutes) } returns flowOf(Result.success(newSlots))

        // init'te 1, onDateSelected'da 1, toplam 2 çağrı olacak getCurrentUser için
        clearMocks(mockGetCurrentUserUseCase, recordedCalls = true) // Önceki çağrıları temizle
        coEvery { mockGetCurrentUserUseCase() } returns Result.success(testUser) // Tekrar mockla

        viewModel.onDateSelected(newDate)
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertEquals(newDate, finalState.selectedDate)
        assertEquals(newSlots, finalState.availableSlots)
        coVerify(exactly = 1) { mockGetCurrentUserUseCase() } // Sadece onDateSelected içindeki çağrı
        verify(exactly = 1) { mockGetAvailableSlotsUseCase(newDate, testService.durationMinutes) }
    }

    // ... (onSlotSelected, onCustomerNameChanged, onCustomerPhoneChanged testleri aynı kalabilir) ...
    // Bu testlerde `coVerify(exactly = 0) { mockGetCurrentUserUseCase() }` yanlış,
    // çünkü init'te zaten çağrılıyor. Bu testler sadece UI state'ini kontrol ediyor,
    // o yüzden getCurrentUser verify'ına gerek yok veya `atLeast = 1` kullanılabilir.

    @Test
    fun `confirmBooking when form is invalid (blank name), does not call createAppointment`() = runTest {
        initializeViewModel()
        advanceUntilIdle() // init ve ilk slot yükleme (getCurrentUser'ı 1 kez çağırır)

        viewModel.onSlotSelected(testSlots.first())
        viewModel.onCustomerNameChanged("") // İsim boş
        viewModel.onCustomerPhoneChanged(testPhone)
        advanceUntilIdle()

        viewModel.confirmBooking()
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertEquals(R.string.error_name_empty, finalState.nameErrorRes)
        coVerify(exactly = 0) { mockCreateAppointmentUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        // GetCurrentUserUseCase'in `confirmBooking` içinde ekstra çağrılmadığını kontrol etmek için,
        // init'teki çağrıdan sonra çağrı sayısının artmadığını doğrulamak gerekir.
        // Veya `confirmBooking` öncesi `clearMocks(mockGetCurrentUserUseCase, recordedCalls = true)`
        // ve sonra `coVerify(exactly = 0) { mockGetCurrentUserUseCase() }`
    }

    @Test
    fun `confirmBooking when slot not selected, sends snackbar and does not call createAppointment`() = runTest {
        initializeViewModel()
        advanceUntilIdle() // init ve ilk slot yükleme

        viewModel.onCustomerNameChanged(testName)
        viewModel.onCustomerPhoneChanged(testPhone)
        // Slot seçilmedi
        advanceUntilIdle()

        viewModel.eventFlow.test(timeout = 3.seconds) {
            viewModel.confirmBooking()
            advanceUntilIdle()
            val event = awaitItem()
            assertEquals(R.string.error_slot_missing, (event as BookingViewModel.BookingViewEvent.ShowSnackbar).messageId)
        }
        coVerify(exactly = 0) { mockCreateAppointmentUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `confirmBooking when user is not authenticated, sends snackbar and does not create appointment`() = runTest {
        coEvery { mockGetCurrentUserUseCase() } returns Result.success(null) // Kullanıcı yok
        initializeViewModel() // init'te getCurrentUser çağrılacak ama null dönecek, slot yüklenmeyecek.
        advanceUntilIdle()

        viewModel.onSlotSelected(testSlots.first())
        viewModel.onCustomerNameChanged(testName)
        viewModel.onCustomerPhoneChanged(testPhone)
        advanceUntilIdle()

        // clearMocks(mockGetCurrentUserUseCase, recordedCalls = true) // init'teki çağrıyı sayma
        // coEvery { mockGetCurrentUserUseCase() } returns Result.success(null) // Tekrar mockla

        viewModel.eventFlow.test(timeout = 3.seconds) {
            viewModel.confirmBooking()
            advanceUntilIdle()
            val event = awaitItem()
            assertEquals(R.string.error_auth_user_not_found, (event as BookingViewModel.BookingViewEvent.ShowSnackbar).messageId)
        }
        // getCurrentUser `confirmBooking` içinde tekrar çağrıldı (toplamda 2. kez veya clearMocks sonrası 1. kez)
        coVerify(atLeast = 1) { mockGetCurrentUserUseCase() }
        coVerify(exactly = 0) { mockCreateAppointmentUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `confirmBooking with valid data and authenticated user, calls use cases and emits navigation event`() = runTest {
        initializeViewModel()
        advanceUntilIdle() // init ve ilk slot yükleme (getCurrentUser'ı 1 kez çağırır)

        val selectedSlot = testSlots.first()
        viewModel.onSlotSelected(selectedSlot)
        viewModel.onCustomerNameChanged(testName)
        viewModel.onCustomerPhoneChanged(testPhone)
        viewModel.onCustomerEmailChanged(testEmail)
        advanceUntilIdle()

        // setUp'ta getCurrentUser ve createAppointment başarılı dönüyor

        viewModel.eventFlow.test(timeout = 3.seconds) {
            viewModel.confirmBooking()
            advanceUntilIdle()
            assertEquals(BookingViewModel.BookingViewEvent.NavigateToConfirmation, awaitItem())
        }

        // getCurrentUser, init'te 1 kez, confirmBooking'de 1 kez çağrıldı.
        coVerify(exactly = 2) { mockGetCurrentUserUseCase() }
        coVerify(exactly = 1) {
            mockCreateAppointmentUseCase(
                userId = eq(testUserId),
                serviceId = eq(testServiceId),
                serviceName = eq(testService.name),
                serviceDuration = eq(testService.durationMinutes),
                date = eq(testDate),
                time = eq(selectedSlot),
                customerName = eq(testName),
                customerPhone = eq(testPhone),
                customerEmail = eq(testEmail)
            )
        }
    }

    @Test
    fun `confirmBooking fails when createAppointmentUseCase fails, sends snackbar event`() = runTest {
        val exceptionFromUseCase = Exception("Create error")
        coEvery {
            mockCreateAppointmentUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.failure(exceptionFromUseCase)
        // getCurrentUser başarılı (setUp'ta)

        initializeViewModel()
        advanceUntilIdle() // init ve ilk slot yükleme

        val selectedSlot = testSlots.first()
        viewModel.onSlotSelected(selectedSlot)
        viewModel.onCustomerNameChanged(testName)
        viewModel.onCustomerPhoneChanged(testPhone)
        advanceUntilIdle()

        viewModel.eventFlow.test(timeout = 3.seconds) {
            viewModel.confirmBooking()
            advanceUntilIdle()
            val event = awaitItem()
            assertEquals(R.string.error_booking_failed, (event as BookingViewModel.BookingViewEvent.ShowSnackbar).messageId)
        }
        // getCurrentUser, init'te 1 kez, confirmBooking'de 1 kez çağrıldı.
        coVerify(exactly = 2) { mockGetCurrentUserUseCase() }
        coVerify(exactly = 1) { mockCreateAppointmentUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `loadAvailableSlotsForDate when serviceDuration is invalid emits ShowSnackbar`() = runTest {
        val serviceWithZeroDuration = Service(id = testServiceId, name = "Zero Dur", durationMinutes = 0)
        coEvery { mockGetServiceDetailsUseCase(testServiceId) } returns Result.success(serviceWithZeroDuration)
        // getCurrentUser başarılı (setUp'ta)

        initializeViewModel() // Bu, loadServiceDetails -> loadAvailableSlotsForDate'i tetikler
        advanceUntilIdle()

        // Assert - StateFlow
        val finalState = viewModel.uiState.value
        assertEquals(0, finalState.serviceDuration)
        assertTrue("Available slots should be empty", finalState.availableSlots.isEmpty())
        assertFalse("isLoadingSlots should be false", finalState.isLoadingSlots)
        // ViewModel'da uiState.slotsErrorMessage = R.string.error_service_duration_not_available yapıldığını varsayıyoruz
        assertEquals(R.string.error_service_duration_not_available, finalState.slotsErrorMessage)

        verify(exactly = 0) { mockGetAvailableSlotsUseCase(any(), any()) }
        coVerify(exactly = 1) { mockGetCurrentUserUseCase() }
    }
}