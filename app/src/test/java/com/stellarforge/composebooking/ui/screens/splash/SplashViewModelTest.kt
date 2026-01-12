package com.stellarforge.composebooking.ui.screens.splash

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.utils.UserPrefs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [SplashViewModel].
 *
 * Verifies the routing logic based on:
 * 1. Local Cache (UserPrefs) - For instant launch.
 * 2. Remote Authentication State (UseCase) - Fallback if cache is empty.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockGetCurrentUserUseCase: GetCurrentUserUseCase

    @RelaxedMockK
    private lateinit var mockUserPrefs: UserPrefs

    private lateinit var viewModel: SplashViewModel

    @Before
    fun setUp() {
        // Default behavior: Simulate an empty cache (Cache Miss).
        // This ensures tests default to the Network Logic unless specified otherwise.
        every { mockUserPrefs.getUserRole() } returns null
    }

    @Test
    fun `when cache is empty AND user is NOT logged in - navigates to Login`() = runTest {
        // ARRANGE
        // Cache miss (set in setUp)
        // Network check: User not found (null)
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(null)

        // ACT
        viewModel = SplashViewModel(mockGetCurrentUserUseCase, mockUserPrefs)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        assertThat(viewModel.startDestination.value).isEqualTo(ScreenRoutes.Login.route)
    }

    @Test
    fun `when cache is empty BUT user is CUSTOMER (Network) - navigates to ServiceList`() = runTest {
        // ARRANGE
        val customer = AuthUser("uid1", "cust@test.com", role = "customer")
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(customer)

        // ACT
        viewModel = SplashViewModel(mockGetCurrentUserUseCase, mockUserPrefs)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        assertThat(viewModel.startDestination.value).isEqualTo(ScreenRoutes.ServiceList.route)
    }

    @Test
    fun `when cache is empty BUT user is OWNER (Network) - navigates to Schedule`() = runTest {
        // ARRANGE
        val owner = AuthUser("uid2", "owner@test.com", role = "owner")
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(owner)

        // ACT
        viewModel = SplashViewModel(mockGetCurrentUserUseCase, mockUserPrefs)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        assertThat(viewModel.startDestination.value).isEqualTo(ScreenRoutes.Schedule.route)
    }

    @Test
    fun `when CACHE HIT (Owner) - navigates immediately to Schedule without network`() = runTest {
        // ARRANGE
        // Simulate that the role is already saved on the device
        every { mockUserPrefs.getUserRole() } returns "owner"

        // ACT
        viewModel = SplashViewModel(mockGetCurrentUserUseCase, mockUserPrefs)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        assertThat(viewModel.startDestination.value).isEqualTo(ScreenRoutes.Schedule.route)

        // CRITICAL CHECK:
        // Since we found the role in cache, the app should NEVER call the network UseCase.
        // This verifies the "Offline-First" optimization.
        coVerify(exactly = 0) { mockGetCurrentUserUseCase() }
    }

    @Test
    fun `when auth check fails (Error) - navigates to Login`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Error(Exception("Network error"))

        // ACT
        viewModel = SplashViewModel(mockGetCurrentUserUseCase, mockUserPrefs)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        // Safety fallback: Redirect to Login on error
        assertThat(viewModel.startDestination.value).isEqualTo(ScreenRoutes.Login.route)
    }
}