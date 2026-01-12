package com.stellarforge.composebooking.ui.screens.customerprofile.edit

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.CustomerProfile
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetCustomerProfileUseCase
import com.stellarforge.composebooking.domain.usecase.UpdateCustomerProfileUseCase
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [EditCustomerProfileViewModel].
 *
 * Checks if the UI State correctly reacts to Success and Error scenarios
 * from the Domain layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditCustomerProfileViewModelTest {

    // 1. Set the Main Dispatcher for Coroutines (Required for ViewModelScope)
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // 2. Mocks
    private val getCurrentUserUseCase: GetCurrentUserUseCase = mockk()
    private val updateCustomerProfileUseCase: UpdateCustomerProfileUseCase = mockk()
    private val getCustomerProfileUseCase: GetCustomerProfileUseCase = mockk()

    private lateinit var viewModel: EditCustomerProfileViewModel

    @Before
    fun setUp() {
        // Default behavior: User is logged in
        coEvery { getCurrentUserUseCase() } returns Result.Success(
            AuthUser(uid = "test_uid", email = "test@example.com", role = "customer")
        )

        // YENİ: ViewModel init bloğunda profil verisini çekmeye çalışıyor.
        // Testin çökmemesi için sahte bir akış (Flow) dönüyoruz.
        coEvery { getCustomerProfileUseCase("test_uid") } returns flowOf(
            Result.Success(CustomerProfile(name = "Old Name", phone = "Old Phone"))
        )

        // Initialize ViewModel with NEW Constructor
        viewModel = EditCustomerProfileViewModel(
            getCurrentUserUseCase,
            getCustomerProfileUseCase,
            updateCustomerProfileUseCase
        )
    }

    @Test
    fun `saveProfile SUCCESS - updates state to isSaved`() = runTest {
        // ARRANGE
        val newName = "Jane Doe"
        val newPhone = "555-9999"

        // Setup user input
        viewModel.onNameChange(newName)
        viewModel.onPhoneChange(newPhone)

        // Mock the UseCase to return Success
        coEvery {
            updateCustomerProfileUseCase(any(), any(), any())
        } returns Result.Success(Unit)

        // ACT
        viewModel.saveProfile()

        // Wait for coroutines to finish
        advanceUntilIdle()

        // ASSERT
        val state = viewModel.uiState.value

        // 1. Loading should be finished
        assertThat(state.isLoading).isFalse()
        // 2. Saved flag should be true (Triggers navigation)
        assertThat(state.isSaved).isTrue()
        // 3. No errors
        assertThat(state.errorResId).isNull()
    }

    @Test
    fun `saveProfile ERROR - updates state with error message`() = runTest {
        // ARRANGE
        viewModel.onNameChange("Jane")
        viewModel.onPhoneChange("123")

        // Mock the UseCase to return Error (Network failure)
        coEvery {
            updateCustomerProfileUseCase(any(), any(), any())
        } returns Result.Error(Exception("Network Error"))

        // ACT
        viewModel.saveProfile()
        advanceUntilIdle()

        // ASSERT
        val state = viewModel.uiState.value

        // 1. Loading finished
        assertThat(state.isLoading).isFalse()
        // 2. Not saved
        assertThat(state.isSaved).isFalse()
        // 3. Error message is set correctly (Generic Error)
        assertThat(state.errorResId).isEqualTo(R.string.error_generic_unknown)
    }

    @Test
    fun `saveProfile VALIDATION ERROR - updates state with specific error`() = runTest {
        // ARRANGE
        viewModel.onNameChange("") // Empty name

        // Mock validation error from UseCase
        coEvery {
            updateCustomerProfileUseCase(any(), any(), any())
        } returns Result.Error(IllegalArgumentException("Validation failed"))

        // ACT
        viewModel.saveProfile()
        advanceUntilIdle()

        // ASSERT
        val state = viewModel.uiState.value

        // Should show the "Generic Problem" error for validation issues (as defined in ViewModel)
        assertThat(state.errorResId).isEqualTo(R.string.error_booking_generic_problem)
    }
}