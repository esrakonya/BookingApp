package com.stellarforge.composebooking.ui.screens.businessprofile

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.domain.usecase.GetBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
import com.stellarforge.composebooking.domain.usecase.UpdateBusinessProfileUseCase
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [BusinessProfileViewModel].
 *
 * Verifies the loading and updating logic for the Business Profile screen.
 * Ensures that UI state correctly reflects Success/Error conditions based on UseCase results.
 */
@ExperimentalCoroutinesApi
class BusinessProfileViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @RelaxedMockK
    private lateinit var getBusinessProfileUseCase: GetBusinessProfileUseCase

    @RelaxedMockK
    private lateinit var updateBusinessProfileUseCase: UpdateBusinessProfileUseCase

    @RelaxedMockK
    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    @RelaxedMockK
    private lateinit var signOutUseCase: SignOutUseCase

    private lateinit var viewModel: BusinessProfileViewModel

    private val fakeAuthUser = AuthUser(uid = "test_user_id", email = "test@test.com")
    private val fakeBusinessProfile = BusinessProfile(businessName = "Test Salon", contactEmail = "contact@test.com")

    private fun createViewModel() {
        viewModel = BusinessProfileViewModel(
            getBusinessProfileUseCase = getBusinessProfileUseCase,
            updateBusinessProfileUseCase = updateBusinessProfileUseCase,
            getCurrentUserUseCase = getCurrentUserUseCase,
            signOutUseCase = signOutUseCase
        )
    }

    @Test
    fun `init - when use cases succeed - loads profile and updates form fields`() = runTest {
        // ARRANGE
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        every { getBusinessProfileUseCase(fakeAuthUser.uid) } returns flowOf(Result.Success(fakeBusinessProfile))

        // ACT
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val finalState = viewModel.uiState.value
        assertThat(finalState.isLoadingProfile).isFalse()
        assertThat(finalState.profileData).isEqualTo(fakeBusinessProfile)
        assertThat(finalState.loadErrorResId).isNull()

        // Verify Form Fields are pre-filled
        assertThat(viewModel.businessName.value).isEqualTo(fakeBusinessProfile.businessName)
        assertThat(viewModel.contactEmail.value).isEqualTo(fakeBusinessProfile.contactEmail)
    }

    @Test
    fun `init - when no existing profile - uiState is success with null data`() = runTest {
        // ARRANGE
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        every { getBusinessProfileUseCase(fakeAuthUser.uid) } returns flowOf(Result.Success(null))

        // ACT
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val finalState = viewModel.uiState.value
        assertThat(finalState.isLoadingProfile).isFalse()
        assertThat(finalState.profileData).isNull()
        assertThat(finalState.loadErrorResId).isNull()
        assertThat(viewModel.businessName.value).isEmpty()
    }

    @Test
    fun `init - when getCurrentUser fails - uiState shows error`() = runTest {
        // ARRANGE
        coEvery { getCurrentUserUseCase() } returns Result.Error(Exception("Auth error"))

        // ACT
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val finalState = viewModel.uiState.value
        assertThat(finalState.isLoadingProfile).isFalse()
        assertThat(finalState.loadErrorResId).isEqualTo(R.string.error_user_not_found_generic)
    }

    @Test
    fun `init - when getUser succeeds but getProfile fails - uiState shows load error`() = runTest {
        // ARRANGE
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)

        every { getBusinessProfileUseCase(fakeAuthUser.uid) } returns flowOf(
            Result.Error(Exception("DB Error"))
        )

        // ACT
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val finalState = viewModel.uiState.value
        assertThat(finalState.isLoadingProfile).isFalse()
        assertThat(finalState.profileData).isNull()
        assertThat(finalState.loadErrorResId).isEqualTo(R.string.error_loading_data_firestore)
    }

    @Test
    fun `saveBusinessProfile - when successful - shows success and reloads profile`() = runTest {
        // ARRANGE
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        val updatedProfile = fakeBusinessProfile.copy(businessName = "Updated Salon")

        // Sequential Behavior: Return old profile first, then updated profile
        every { getBusinessProfileUseCase(fakeAuthUser.uid) } returns flowOf(Result.Success(fakeBusinessProfile)) andThen flowOf(Result.Success(updatedProfile))
        coEvery { updateBusinessProfileUseCase(any()) } returns Result.Success(Unit)

        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle() // Finish init

        // ACT
        viewModel.onBusinessNameChanged("Updated Salon")
        viewModel.saveBusinessProfile()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle() // Finish save

        // ASSERT
        val finalState = viewModel.uiState.value
        assertThat(finalState.isUpdatingProfile).isFalse()
        assertThat(finalState.updateSuccessResId).isEqualTo(R.string.profile_update_success)
        assertThat(viewModel.businessName.value).isEqualTo("Updated Salon")
    }

    @Test
    fun `saveBusinessProfile - with blank business name - shows validation error`() = runTest {
        // ARRANGE
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        every { getBusinessProfileUseCase(fakeAuthUser.uid) } returns flowOf(Result.Success(null))
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ACT
        viewModel.onBusinessNameChanged("   ") // Blank Name
        viewModel.saveBusinessProfile()

        // ASSERT
        val state = viewModel.uiState.value
        assertThat(state.isUpdatingProfile).isFalse()
        assertThat(state.updateErrorResId).isEqualTo(R.string.label_business_name_required)

        coVerify(exactly = 0) { updateBusinessProfileUseCase(any()) }
    }

    @Test
    fun `saveBusinessProfile - when updateUseCase fails - uiState shows update error`() = runTest {
        // ARRANGE
        val expectedErrorMessage = "Profil güncellenemedi." // Simulating a raw error message if needed, or mapping logic

        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        every { getBusinessProfileUseCase(any()) } returns flowOf(Result.Success(null))

        // Mock error with specific message
        coEvery { updateBusinessProfileUseCase(any()) } returns Result.Error(
            exception = Exception("Technical error"),
            message = expectedErrorMessage
        )

        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ACT
        viewModel.onBusinessNameChanged("Valid Name")
        viewModel.saveBusinessProfile()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val state = viewModel.uiState.value
        assertThat(state.isUpdatingProfile).isFalse()

        // Asserting the resource ID mapped in the ViewModel
        assertThat(state.updateErrorResId).isEqualTo(R.string.profile_update_failed)
    }
}