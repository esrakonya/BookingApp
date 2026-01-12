package com.stellarforge.composebooking.ui.screens.customerprofile

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.data.model.CustomerProfile
import com.stellarforge.composebooking.domain.usecase.GetBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetCustomerProfileUseCase
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [CustomerProfileViewModel].
 *
 * Tests the initialization of user profile data and the logout flow.
 */
@ExperimentalCoroutinesApi
class CustomerProfileViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @RelaxedMockK
    private lateinit var mockSignOutUseCase: SignOutUseCase

    @RelaxedMockK
    private lateinit var mockGetCurrentUserUseCase: GetCurrentUserUseCase

    @RelaxedMockK
    private lateinit var mockGetBusinessProfileUseCase: GetBusinessProfileUseCase

    @RelaxedMockK
    private lateinit var mockGetCustomerProfileUseCase: GetCustomerProfileUseCase

    private lateinit var viewModel: CustomerProfileViewModel

    private val testUser = AuthUser("uid_123", "musteri@test.com")
    private val testCustomerProfile = CustomerProfile(id = "uid_123", name="Customer Test", phone = "555")
    private val testBusinessProfile = BusinessProfile(businessName = "Test Barber", contactPhone = "123")

    private fun createViewModel() {
        viewModel = CustomerProfileViewModel(
            mockSignOutUseCase,
            mockGetCurrentUserUseCase,
            mockGetCustomerProfileUseCase,
            mockGetBusinessProfileUseCase
        )
    }

    @Test
    fun `init - loads user and business profile successfully`() = runTest {
        // ARRANGE
        // 1. Auth returns success
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(testUser)

        // 2. Profile Stream returns success with data
        every { mockGetCustomerProfileUseCase("uid_123") } returns flowOf(Result.Success(testCustomerProfile))

        // 3. Business Profile returns success
        every { mockGetBusinessProfileUseCase(FirebaseConstants.TARGET_BUSINESS_OWNER_ID) } returns flowOf(Result.Success(testBusinessProfile))

        // ACT
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val state = viewModel.uiState.value

        // Check Loading
        assertThat(state.isLoading).isFalse()

        // Check Auth Data
        assertThat(state.userEmail).isEqualTo("musteri@test.com")

        // Check Firestore Data (Real-time name)
        // ViewModel logic now takes the name from Firestore Profile if available
        assertThat(state.userName).isEqualTo("Customer Test")

        // Check Business Info
        assertThat(state.businessProfile).isEqualTo(testBusinessProfile)
        assertThat(state.errorMessageId).isNull()
    }

    @Test
    fun `init - when user not found (Auth Error) - shows error message`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(null)
        // If auth fails, profile stream is never called, so we don't strictly need to mock it here,
        // but relaxing mocks handles it safely.

        // ACT
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessageId).isEqualTo(R.string.error_user_not_found)
    }

    @Test
    fun `signOut - calls usecase and emits NavigateToLogin`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(testUser)
        every { mockGetBusinessProfileUseCase(any()) } returns flowOf(Result.Success(null))
        coEvery { mockSignOutUseCase() } returns Result.Success(Unit)

        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ACT & ASSERT
        viewModel.eventFlow.test {
            viewModel.signOut()

            // Verify Navigation Event
            assertThat(awaitItem()).isEqualTo(CustomerProfileEvent.NavigateToLogin)

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { mockSignOutUseCase() }
    }
}