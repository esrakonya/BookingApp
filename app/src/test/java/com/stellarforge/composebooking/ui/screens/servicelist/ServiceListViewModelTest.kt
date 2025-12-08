package com.stellarforge.composebooking.ui.screens.servicelist

import app.cash.turbine.test
import com.google.firebase.firestore.FirebaseFirestoreException
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.domain.usecase.GetBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for [ServiceListViewModel].
 *
 * Tests the Customer Home Screen logic, including:
 * - Fetching the list of available services.
 * - Loading the business profile.
 * - Error states.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServiceListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockServiceRepository: ServiceRepository

    @RelaxedMockK
    private lateinit var mockAppointmentRepository: AppointmentRepository

    @RelaxedMockK
    private lateinit var mockSignOutUseCase: SignOutUseCase

    @RelaxedMockK
    private lateinit var mockGetCurrentUserUseCase: GetCurrentUserUseCase

    @RelaxedMockK
    private lateinit var mockGetBusinessProfileUseCase: GetBusinessProfileUseCase

    private lateinit var viewModel: ServiceListViewModel

    private val fakeAuthUser = AuthUser("test_uid", "test@test.com")
    private val fakeServices = listOf(Service(id = "s1", name = "Service 1"))

    @Before
    fun setUp() {
        // Mock static Firebase Exception to prevent initialization errors in unit tests
        mockkStatic(FirebaseFirestoreException.Code::class)
        every { FirebaseFirestoreException.Code.values() } returns emptyArray()

        // Redirect Timber logs to System.out for test debugging
        Timber.uprootAll()
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                println("TIMBER: $message")
            }
        })
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseFirestoreException.Code::class)
        Timber.uprootAll()
    }

    private fun createViewModel() {
        viewModel = ServiceListViewModel(
            serviceRepository = mockServiceRepository,
            appointmentRepository = mockAppointmentRepository,
            signOutUseCase = mockSignOutUseCase,
            getCurrentUserUseCase = mockGetCurrentUserUseCase,
            getBusinessProfileUseCase = mockGetBusinessProfileUseCase
        )
    }

    @Test
    fun `init - when user exists - loads services and business profile successfully`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(fakeAuthUser)

        // Using constant TARGET_ID for business profile
        every { mockGetBusinessProfileUseCase(FirebaseConstants.TARGET_BUSINESS_OWNER_ID) } returns flowOf(Result.Success(null))

        // Ensure we call the customer stream
        every { mockServiceRepository.getCustomerServicesStream() } returns flowOf(Result.Success(fakeServices))

        // ACT
        createViewModel()

        // ASSERT
        viewModel.uiState.test {
            // First: Loading
            assertEquals(ServiceListUiState.Loading, awaitItem())

            // Second: Success
            val successState = awaitItem()
            assertTrue("State should be Success", successState is ServiceListUiState.Success)
            assertEquals(fakeServices, (successState as ServiceListUiState.Success).services)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { mockGetCurrentUserUseCase() }
        verify(exactly = 1) { mockGetBusinessProfileUseCase(FirebaseConstants.TARGET_BUSINESS_OWNER_ID) }
        verify(exactly = 1) { mockServiceRepository.getCustomerServicesStream() }
    }

    @Test
    fun `init - when getCurrentUser fails - uiState is Error`() = runTest {
        // ARRANGE
        val authException = Exception("Auth failed")

        // 1. User check fails
        coEvery { mockGetCurrentUserUseCase() } returns Result.Error(authException)

        every { mockGetBusinessProfileUseCase(any()) } returns flowOf(Result.Success(null))

        // ACT
        createViewModel()
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // ASSERT
        var currentState = viewModel.uiState.value

        assertTrue("Final state should be Error", currentState is ServiceListUiState.Error)
        assertEquals(
            R.string.error_auth_user_not_found_for_services,
            (currentState as ServiceListUiState.Error).messageResId
        )

        // Services should NOT be fetched if auth fails
        coVerify(exactly = 0) { mockServiceRepository.getCustomerServicesStream() }
    }

    @Test
    fun `onRetryClicked - reloads initial data`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        every { mockGetBusinessProfileUseCase(any()) } returns flowOf(Result.Success(null))
        every { mockServiceRepository.getCustomerServicesStream() } returns flowOf(Result.Success(fakeServices))

        createViewModel()

        // Consume initial flow
        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // Success
            cancelAndIgnoreRemainingEvents()
        }

        // ACT
        viewModel.onRetryClicked()

        // ASSERT
        viewModel.uiState.test {
            // Loading again
            assertEquals(ServiceListUiState.Loading, awaitItem())
            // Success again
            assertTrue(awaitItem() is ServiceListUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }

        // Verify it was called twice
        coVerify(exactly = 2) { mockGetCurrentUserUseCase() }
    }

    @Test
    fun `signOut - when use case succeeds - emits NavigateToLogin event`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        every { mockServiceRepository.getCustomerServicesStream() } returns flowOf(Result.Success(emptyList()))
        every { mockGetBusinessProfileUseCase(any()) } returns flowOf(Result.Success(null))
        coEvery { mockSignOutUseCase() } returns Result.Success(Unit)

        createViewModel()

        // ACT & ASSERT
        viewModel.eventFlow.test(timeout = 3.seconds) {
            viewModel.signOut()
            assertEquals(ServiceListViewEvent.NavigateToLogin, awaitItem())
            cancelAndConsumeRemainingEvents()
        }

        coVerify(exactly = 1) { mockSignOutUseCase() }
    }
}