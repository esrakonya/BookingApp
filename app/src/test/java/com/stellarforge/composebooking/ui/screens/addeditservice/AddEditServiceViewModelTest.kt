package com.stellarforge.composebooking.ui.screens.addeditservice

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.domain.usecase.*
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [AddEditServiceViewModel].
 *
 * Verifies the logic for:
 * 1. Form Validation (Name, Price, Duration).
 * 2. Data Conversion (Currency formatting).
 * 3. Interaction with UseCases (Adding vs Editing services).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditServiceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK private lateinit var getServiceDetailsUseCase: GetServiceDetailsUseCase
    @RelaxedMockK private lateinit var addServiceUseCase: AddServiceUseCase
    @RelaxedMockK private lateinit var updateServiceUseCase: UpdateServiceUseCase
    @RelaxedMockK private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    private lateinit var viewModel: AddEditServiceViewModel
    private val fakeUser = AuthUser("uid1", "test@test.com")

    @Before
    fun setUp() {
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeUser)
        coEvery { addServiceUseCase(any()) } returns Result.Success(Unit)

        // Scenario: Add Mode (serviceId is null)
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditServiceViewModel(savedStateHandle, getServiceDetailsUseCase, addServiceUseCase, updateServiceUseCase, getCurrentUserUseCase)
    }

    @Test
    fun `saveService - with empty name - sets name error`() = runTest {
        // ARRANGE
        viewModel.onNameChange("")
        viewModel.onPriceChange("100")
        viewModel.onDurationChange("30")

        // ACT
        viewModel.saveService()

        // Wait for coroutine to complete
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        assertThat(viewModel.uiState.value.error).isEqualTo(R.string.error_name_empty)
        coVerify(exactly = 0) { addServiceUseCase(any()) }
    }

    @Test
    fun `saveService - with invalid duration (0) - sets generic error`() = runTest {
        // ARRANGE
        viewModel.onNameChange("Valid Name")
        viewModel.onPriceChange("100")
        viewModel.onDurationChange("0")

        // ACT
        viewModel.saveService()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        assertThat(viewModel.uiState.value.error).isEqualTo(R.string.error_booking_generic_problem)
        coVerify(exactly = 0) { addServiceUseCase(any()) }
    }

    @Test
    fun `saveService - with valid input - converts price and calls useCase`() = runTest {
        // ARRANGE
        viewModel.onNameChange("Haircut")
        viewModel.onDurationChange("30")
        viewModel.onPriceChange("100.50") // Input: 100.50 Currency Units

        // ACT
        viewModel.saveService()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        assertThat(viewModel.uiState.value.serviceSaved).isTrue()

        // Validation: Verify price is converted to cents (100.50 -> 10050) to avoid floating point errors
        coVerify(exactly = 1) {
            addServiceUseCase(match { it.priceInCents == 10050L && it.name == "Haircut" })
        }
    }
}