package com.stellarforge.composebooking.ui.screens.customerprofile

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.domain.usecase.GetBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
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

    private lateinit var viewModel: CustomerProfileViewModel

    private val testUser = AuthUser("uid_123", "musteri@test.com")
    private val testBusinessProfile = BusinessProfile(businessName = "Test Barber", contactPhone = "123")

    private fun createViewModel() {
        viewModel = CustomerProfileViewModel(
            mockSignOutUseCase,
            mockGetCurrentUserUseCase,
            mockGetBusinessProfileUseCase
        )
    }

    @Test
    fun `init - loads user and business profile successfully`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(testUser)
        every { mockGetBusinessProfileUseCase(FirebaseConstants.TARGET_BUSINESS_OWNER_ID) } returns flowOf(Result.Success(testBusinessProfile))

        // ACT
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val state = viewModel.uiState.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.userEmail).isEqualTo("musteri@test.com")

        // According to ViewModel logic, username is parsed from email (before @)
        assertThat(state.userName).isEqualTo("musteri")

        assertThat(state.businessProfile).isEqualTo(testBusinessProfile)
        assertThat(state.errorMessageId).isNull()
    }

    @Test
    fun `init - when user not found - shows error message`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(null)
        every { mockGetBusinessProfileUseCase(any()) } returns flowOf(Result.Success(null))

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