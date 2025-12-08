package com.stellarforge.composebooking.ui.screens.manageservices

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.usecase.DeleteServiceUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetOwnerServicesUseCase
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

/**
 * Unit tests for [ManageServicesViewModel].
 *
 * Verifies that the Business Owner can view and delete their services correctly.
 */
@ExperimentalCoroutinesApi
class ManageServicesViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @RelaxedMockK
    private lateinit var getOwnerServicesUseCase: GetOwnerServicesUseCase

    @RelaxedMockK
    private lateinit var deleteServiceUseCase: DeleteServiceUseCase

    @RelaxedMockK
    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    private lateinit var viewModel: ManageServicesViewModel

    private val testUser = AuthUser(uid = "test-owner-id")
    private val testServices = listOf(
        Service(id = "s1", name = "Service A", ownerId = testUser.uid),
        Service(id = "s2", name = "Service B", ownerId = testUser.uid)
    )

    @Before
    fun setUp() {
        coEvery { getCurrentUserUseCase() } returns Result.Success(testUser)
        every { getOwnerServicesUseCase(testUser.uid) } returns flowOf(Result.Success(testServices))
        coEvery { deleteServiceUseCase(any()) } returns Result.Success(Unit)
    }

    private fun createViewModel() {
        viewModel = ManageServicesViewModel(
            getOwnerServicesUseCase,
            deleteServiceUseCase,
            getCurrentUserUseCase
        )
    }

    @Test
    fun `init - when user is authenticated - loads services successfully`() = runTest {
        createViewModel()

        viewModel.uiState.test {
            // Turbine may catch initial loading state
            val firstItem = awaitItem()
            if (firstItem.isLoading) {
                // Wait for success
                val successState = awaitItem()
                assertThat(successState.isLoading).isFalse()
                assertThat(successState.services).isEqualTo(testServices)
                assertThat(successState.errorResId).isNull()
            } else {
                // If it skipped to success immediately
                assertThat(firstItem.services).isEqualTo(testServices)
            }
        }

        coVerify(exactly = 1) { getCurrentUserUseCase() }
        verify(exactly = 1) { getOwnerServicesUseCase(testUser.uid) }
    }

    @Test
    fun `init - when user is not authenticated - sets error state`() = runTest {
        // ARRANGE: No User
        coEvery { getCurrentUserUseCase() } returns Result.Success(null)

        createViewModel()

        viewModel.uiState.test {
            val item1 = awaitItem()
            val errorState = if(item1.isLoading) awaitItem() else item1

            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.services).isEmpty()
            assertThat(errorState.errorResId).isEqualTo(R.string.error_user_not_found_generic)
        }

        verify(exactly = 0) { getOwnerServicesUseCase(any()) }
    }

    @Test
    fun `init - when getOwnerServicesUseCase fails - sets error state`() = runTest {
        // ARRANGE: DB Error
        val exception = Exception("DB error")
        every { getOwnerServicesUseCase(testUser.uid) } returns flowOf(Result.Error(exception))

        createViewModel()

        viewModel.uiState.test {
            val item1 = awaitItem()
            val errorState = if(item1.isLoading) awaitItem() else item1

            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.services).isEmpty()
            assertThat(errorState.errorResId).isEqualTo(R.string.error_services_loading_failed)
        }
    }

    @Test
    fun `deleteService - when successful - calls use case and updates deleting state`() = runTest {
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            skipItems(1) // Skip current state

            // ACT
            viewModel.deleteService("s1")

            // ASSERT
            // 1. Deleting state
            val deletingState = awaitItem()
            assertThat(deletingState.isDeletingServiceId).isEqualTo("s1")

            // 2. Final state (deletion complete)
            val finalState = awaitItem()
            assertThat(finalState.isDeletingServiceId).isNull()
            assertThat(finalState.errorResId).isNull()
        }

        coVerify(exactly = 1) { deleteServiceUseCase("s1") }
    }

    @Test
    fun `deleteService - when use case fails - sets error state and resets deleting state`() = runTest {
        // ARRANGE
        val exception = Exception("Deletion failed")
        coEvery { deleteServiceUseCase("s1") } returns Result.Error(exception)

        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            skipItems(1)

            // ACT
            viewModel.deleteService("s1")

            // ASSERT
            // 1. Deleting state
            assertThat(awaitItem().isDeletingServiceId).isEqualTo("s1")

            // 2. Error state
            val errorState = awaitItem()
            assertThat(errorState.errorResId).isEqualTo(R.string.error_service_deletion_failed)
            assertThat(errorState.isDeletingServiceId).isNull()
        }
    }
}