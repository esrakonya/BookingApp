package com.stellarforge.composebooking.ui.screens.servicelist

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.BusinessProfile
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
 * - Loading the business profile (Branding).
 * - Error handling and Retry logic.
 * - Sign out flow.
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

    // Test Data
    private val fakeAuthUser = AuthUser("test_uid", "test@test.com")
    private val fakeServices = listOf(Service(id = "s1", name = "Haircut"))
    private val fakeBusinessProfile = BusinessProfile(businessName = "The Urban Cut", address = "NYC")

    @Before
    fun setUp() {
        // Mock static Firebase Exception to prevent initialization errors in unit tests
        mockkStatic(FirebaseFirestoreException.Code::class)
        every { FirebaseFirestoreException.Code.values() } returns emptyArray()

        // Redirect Timber logs to System.out for better debugging in tests
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
            appointmentRepository = mockAppointmentRepository, // Not used but required
            serviceRepository = mockServiceRepository,
            signOutUseCase = mockSignOutUseCase,
            getCurrentUserUseCase = mockGetCurrentUserUseCase,
            getBusinessProfileUseCase = mockGetBusinessProfileUseCase
        )
    }

    @Test
    fun `init - when user exists - loads services and business profile successfully`() = runTest {
        // ARRANGE
        // 1. User Check
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(fakeAuthUser)

        // 2. Business Profile Load
        every {
            mockGetBusinessProfileUseCase(FirebaseConstants.TARGET_BUSINESS_OWNER_ID)
        } returns flowOf(Result.Success(fakeBusinessProfile))

        // 3. Service List Load
        every { mockServiceRepository.getCustomerServicesStream() } returns flowOf(Result.Success(fakeServices))

        // ACT
        createViewModel()

        // ASSERT UI STATE
        viewModel.uiState.test {
            // First: Loading
            assertThat(awaitItem()).isEqualTo(ServiceListUiState.Loading)

            // Second: Success
            val successState = awaitItem()
            assertThat(successState).isInstanceOf(ServiceListUiState.Success::class.java)
            assertThat((successState as ServiceListUiState.Success).services).isEqualTo(fakeServices)

            cancelAndIgnoreRemainingEvents()
        }

        // ASSERT BUSINESS PROFILE STATE (Separate Flow)
        val profileState = viewModel.businessProfile.value
        assertThat(profileState).isNotNull()
        assertThat(profileState?.businessName).isEqualTo(fakeBusinessProfile.businessName)

        // VERIFY CALLS
        coVerify(exactly = 1) { mockGetCurrentUserUseCase() }
        verify(exactly = 1) { mockGetBusinessProfileUseCase(FirebaseConstants.TARGET_BUSINESS_OWNER_ID) }
        verify(exactly = 1) { mockServiceRepository.getCustomerServicesStream() }
    }

    @Test
    fun `init - when getCurrentUser fails - uiState is Error`() = runTest {
        // ARRANGE
        val authException = Exception("Auth failed")

        // User check fails
        coEvery { mockGetCurrentUserUseCase() } returns Result.Error(authException)

        // Profile might still load (independent flow), let's say it returns null for this test
        every { mockGetBusinessProfileUseCase(any()) } returns flowOf(Result.Success(null))

        // ACT
        createViewModel()

        // Wait for coroutines to settle
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // ASSERT
        val currentState = viewModel.uiState.value

        assertThat(currentState).isInstanceOf(ServiceListUiState.Error::class.java)

        // It should return the specific error resource for auth failure
        val errorState = currentState as ServiceListUiState.Error
        assertThat(errorState.messageResId).isEqualTo(R.string.error_auth_user_not_found_for_services)

        // Services should NOT be fetched if auth fails
        verify(exactly = 0) { mockServiceRepository.getCustomerServicesStream() }
    }

    @Test
    fun `onRetryClicked - reloads initial data`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        every { mockGetBusinessProfileUseCase(any()) } returns flowOf(Result.Success(null))
        every { mockServiceRepository.getCustomerServicesStream() } returns flowOf(Result.Success(fakeServices))

        createViewModel()

        // Consume initial flow to clear the buffer
        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // Success
            cancelAndIgnoreRemainingEvents()
        }

        // ACT
        viewModel.onRetryClicked()

        // ASSERT
        viewModel.uiState.test {
            // Should emit Loading again
            assertThat(awaitItem()).isEqualTo(ServiceListUiState.Loading)
            // Then Success again
            assertThat(awaitItem()).isInstanceOf(ServiceListUiState.Success::class.java)
            cancelAndIgnoreRemainingEvents()
        }

        // Verify it was called twice (Initial + Retry)
        coVerify(exactly = 2) { mockGetCurrentUserUseCase() }
    }

    @Test
    fun `signOut - when use case succeeds - emits NavigateToLogin event`() = runTest {
        // ARRANGE
        // Setup successful init to avoid crashes
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        every { mockServiceRepository.getCustomerServicesStream() } returns flowOf(Result.Success(emptyList()))
        every { mockGetBusinessProfileUseCase(any()) } returns flowOf(Result.Success(null))

        // Sign Out Success
        coEvery { mockSignOutUseCase() } returns Result.Success(Unit)

        createViewModel()

        // ACT & ASSERT
        viewModel.eventFlow.test(timeout = 3.seconds) {
            viewModel.signOut()

            // Verify Navigation Event
            assertThat(awaitItem()).isEqualTo(ServiceListViewEvent.NavigateToLogin)

            cancelAndConsumeRemainingEvents()
        }

        coVerify(exactly = 1) { mockSignOutUseCase() }
    }
}