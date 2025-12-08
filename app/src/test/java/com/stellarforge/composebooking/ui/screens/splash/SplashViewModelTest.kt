package com.stellarforge.composebooking.ui.screens.splash

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
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
 * Verifies the routing logic based on the user's authentication state and role.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockGetCurrentUserUseCase: GetCurrentUserUseCase

    private lateinit var viewModel: SplashViewModel

    @Test
    fun `when user is NOT logged in - navigates to Login`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(null)

        // ACT
        viewModel = SplashViewModel(mockGetCurrentUserUseCase)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        assertThat(viewModel.startDestination.value).isEqualTo(ScreenRoutes.Login.route)
    }

    @Test
    fun `when user is CUSTOMER - navigates to ServiceList`() = runTest {
        // ARRANGE
        val customer = AuthUser("uid1", "cust@test.com", role = "customer")
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(customer)

        // ACT
        viewModel = SplashViewModel(mockGetCurrentUserUseCase)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        assertThat(viewModel.startDestination.value).isEqualTo(ScreenRoutes.ServiceList.route)
    }

    @Test
    fun `when user is OWNER - navigates to Schedule`() = runTest {
        // ARRANGE
        val owner = AuthUser("uid2", "owner@test.com", role = "owner")
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(owner)

        // ACT
        viewModel = SplashViewModel(mockGetCurrentUserUseCase)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        assertThat(viewModel.startDestination.value).isEqualTo(ScreenRoutes.Schedule.route)
    }

    @Test
    fun `when auth check fails (Error) - navigates to Login`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Error(Exception("Network error"))

        // ACT
        viewModel = SplashViewModel(mockGetCurrentUserUseCase)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        // Safety fallback: Redirect to Login on error
        assertThat(viewModel.startDestination.value).isEqualTo(ScreenRoutes.Login.route)
    }
}