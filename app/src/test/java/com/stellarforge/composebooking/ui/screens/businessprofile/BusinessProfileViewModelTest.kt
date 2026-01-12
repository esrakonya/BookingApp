package com.stellarforge.composebooking.ui.screens.businessprofile

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.domain.usecase.GetBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
import com.stellarforge.composebooking.domain.usecase.UpdateBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.UploadBusinessLogoUseCase
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [BusinessProfileViewModel].
 *
 * This test suite verifies the presentation logic for the Business Profile screen, including:
 * 1. Initial data loading (Profile fetching).
 * 2. Form validation and updates.
 * 3. Error handling (Network/Auth errors).
 * 4. Image upload logic (Logo).
 * 5. Session management (Sign Out).
 */
@ExperimentalCoroutinesApi
class BusinessProfileViewModelTest {

    // Rule to handle Coroutines Main Dispatcher in unit tests
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Rule to initialize MockK annotations
    @get:Rule
    val mockkRule = MockKRule(this)

    // Mocks for UseCases
    @RelaxedMockK private lateinit var getBusinessProfileUseCase: GetBusinessProfileUseCase
    @RelaxedMockK private lateinit var updateBusinessProfileUseCase: UpdateBusinessProfileUseCase
    @RelaxedMockK private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase
    @RelaxedMockK private lateinit var signOutUseCase: SignOutUseCase
    @RelaxedMockK private lateinit var uploadBusinessLogoUseCase: UploadBusinessLogoUseCase

    private lateinit var viewModel: BusinessProfileViewModel

    // Test Data
    private val fakeAuthUser = AuthUser(uid = "test_user_id", email = "test@test.com")
    private val fakeBusinessProfile = BusinessProfile(businessName = "Test Salon", contactEmail = "contact@test.com")

    @Before
    fun setUp() {
        // Default Behavior: Happy Path
        // 1. User is logged in
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        // 2. Profile exists
        every { getBusinessProfileUseCase(fakeAuthUser.uid) } returns flowOf(Result.Success(fakeBusinessProfile))
        // 3. Updates are successful
        coEvery { updateBusinessProfileUseCase(any()) } returns Result.Success(Unit)

        // Initialize the ViewModel with default mocks
        createViewModel()
    }

    /**
     * Helper to instantiate the ViewModel with injected mocks.
     */
    private fun createViewModel() {
        viewModel = BusinessProfileViewModel(
            getBusinessProfileUseCase,
            updateBusinessProfileUseCase,
            getCurrentUserUseCase,
            signOutUseCase,
            uploadBusinessLogoUseCase
        )
    }

    // --- TEST 1: SUCCESSFUL LOAD ---
    @Test
    fun `init - loads profile and updates form fields successfully`() = runTest {
        // ACT
        // ViewModel is already initialized in @Before, wait for coroutines to settle
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val finalState = viewModel.uiState.value

        // Check Loading State
        assertThat(finalState.isLoadingProfile).isFalse()
        // Check Data
        assertThat(finalState.profileData).isEqualTo(fakeBusinessProfile)

        // Check Form Fields Pre-fill
        assertThat(viewModel.businessName.value).isEqualTo(fakeBusinessProfile.businessName)
        assertThat(viewModel.contactEmail.value).isEqualTo(fakeBusinessProfile.contactEmail)
    }

    // --- TEST 2: NEW USER (EMPTY PROFILE) ---
    @Test
    fun `init - when no existing profile - uiState is success with null data`() = runTest {
        // ARRANGE: Profile stream returns null (New Account)
        every { getBusinessProfileUseCase(fakeAuthUser.uid) } returns flowOf(Result.Success(null))

        // ACT: Re-create ViewModel to trigger init block with new mock behavior
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val finalState = viewModel.uiState.value
        assertThat(finalState.isLoadingProfile).isFalse()
        assertThat(finalState.profileData).isNull()
        assertThat(finalState.loadErrorResId).isNull()

        // Form fields should be empty
        assertThat(viewModel.businessName.value).isEmpty()
    }

    // --- TEST 3: AUTH ERROR ---
    @Test
    fun `init - when getCurrentUser fails - shows auth error`() = runTest {
        // ARRANGE: Auth check fails
        coEvery { getCurrentUserUseCase() } returns Result.Error(Exception("Auth Fail"))

        // ACT
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val finalState = viewModel.uiState.value
        assertThat(finalState.loadErrorResId).isEqualTo(R.string.error_user_not_found_generic)
    }

    // --- TEST 4: DATABASE ERROR (LOAD) ---
    @Test
    fun `init - when getProfile fails (DB Error) - shows load error`() = runTest {
        // ARRANGE: Profile stream emits Error
        every { getBusinessProfileUseCase(fakeAuthUser.uid) } returns flowOf(Result.Error(Exception("DB Error")))

        // ACT
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val finalState = viewModel.uiState.value
        assertThat(finalState.isLoadingProfile).isFalse()
        assertThat(finalState.loadErrorResId).isEqualTo(R.string.error_loading_data_firestore)
    }

    // --- TEST 5: SUCCESSFUL SAVE ---
    @Test
    fun `saveBusinessProfile - successful update shows success message`() = runTest {
        // ARRANGE
        viewModel.onBusinessNameChanged("New Name")

        // ACT
        viewModel.saveBusinessProfile()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val state = viewModel.uiState.value
        assertThat(state.isUpdatingProfile).isFalse()
        assertThat(state.updateSuccessResId).isEqualTo(R.string.profile_update_success)

        // Verify UseCase called with correct data
        coVerify { updateBusinessProfileUseCase(match { it.businessName == "New Name" }) }
    }

    // --- TEST 6: VALIDATION ERROR ---
    @Test
    fun `saveBusinessProfile - blank name shows validation error`() = runTest {
        // ACT
        viewModel.onBusinessNameChanged("   ") // Invalid Blank Name
        viewModel.saveBusinessProfile()

        // ASSERT
        val state = viewModel.uiState.value
        assertThat(state.updateErrorResId).isEqualTo(R.string.label_business_name_required)

        // UseCase should NOT be called
        coVerify(exactly = 0) { updateBusinessProfileUseCase(any()) }
    }

    // --- TEST 7: SAVE ERROR (NETWORK/DB) ---
    @Test
    fun `saveBusinessProfile - when update fails - shows error message`() = runTest {
        // ARRANGE
        viewModel.onBusinessNameChanged("Valid Name")
        // UseCase returns Error
        coEvery { updateBusinessProfileUseCase(any()) } returns Result.Error(Exception("Network Error"))

        // ACT
        viewModel.saveBusinessProfile()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val state = viewModel.uiState.value
        assertThat(state.isUpdatingProfile).isFalse()
        assertThat(state.updateErrorResId).isEqualTo(R.string.profile_update_failed)
    }

    // --- TEST 8: LOGO UPLOAD & AUTO-SAVE ---
    @Test
    fun `onLogoSelected - uploads logo AND auto-saves profile`() = runTest {
        // 1. Wait for the initial load to complete
        // This prevents the initial empty profile fetch from overriding our new logo state.
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ARRANGE
        val uri = mockk<Uri>()
        val newLogoUrl = "https://new-logo.com"

        // The expected profile state AFTER the update
        val updatedProfile = fakeBusinessProfile.copy(logoUrl = newLogoUrl)

        // Mock: Upload Success
        coEvery {
            uploadBusinessLogoUseCase(any(), fakeAuthUser.uid)
        } returns Result.Success(newLogoUrl)

        // Mock: Update Success
        coEvery { updateBusinessProfileUseCase(any()) } returns Result.Success(Unit)


        // The ViewModel re-fetches the profile after saving.
        // We must override the mock to return the NEW profile (with the logo)
        // instead of the old one defined in setUp().
        every {
            getBusinessProfileUseCase(fakeAuthUser.uid)
        } returns flowOf(Result.Success(updatedProfile))

        // ACT
        viewModel.onLogoSelected(uri)

        // Wait for all coroutines (Upload -> Save -> Refresh) to finish
        advanceUntilIdle()

        // ASSERT
        // 1. Check if URL state is updated correctly
        assertThat(viewModel.logoUrl.value).isEqualTo(newLogoUrl)

        // 2. Verify that Auto-Save was triggered with the NEW URL
        coVerify(exactly = 1) {
            updateBusinessProfileUseCase(match { it.logoUrl == newLogoUrl })
        }
    }
}