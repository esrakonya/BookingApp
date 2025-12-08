package com.stellarforge.composebooking.ui.screens.auth

import app.cash.turbine.test
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.domain.usecase.SignUpUseCase
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for [OwnerSignUpViewModel].
 *
 * Verifies the registration process for Business Owners, including:
 * - Validation (Passwords must match, Email format).
 * - Correct Role Assignment ('owner').
 * - Navigation upon success.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnerSignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockSignUpUseCase: SignUpUseCase
    private lateinit var viewModel: OwnerSignUpViewModel

    private val testEmail = "newuser@example.com"
    private val testPassword = "password123"
    // IMPORTANT: Testing specifically for 'owner' role
    private val testRole = "owner"
    private val successUser = AuthUser("uid789", testEmail, role = testRole)

    // Mock exceptions
    private val mockNetworkException: FirebaseNetworkException = mockk()
    private val mockUserCollisionException: FirebaseAuthUserCollisionException = mockk()
    private val mockWeakPasswordException: FirebaseAuthWeakPasswordException = mockk()
    private val mockOtherException: Exception = mockk()

    @Before
    fun setUp() {
        mockSignUpUseCase = mockk()
        viewModel = OwnerSignUpViewModel(mockSignUpUseCase)

        // Setup generic error messages for mocks
        every { mockNetworkException.localizedMessage } returns "Mocked Network Error"
        every { mockUserCollisionException.localizedMessage } returns "Mocked Email Collision"
        every { mockWeakPasswordException.localizedMessage } returns "Mocked Weak Password"
        every { mockOtherException.localizedMessage } returns "Some other unknown error"
    }

    // --- State Update Tests ---

    @Test
    fun `onEmailChange updates email in uiState and clears errors`() = runTest {
        viewModel.onEmailChange("")
        viewModel.onSignUpClick() // Trigger error
        assertNotNull(viewModel.uiState.value.emailErrorRes)

        viewModel.onEmailChange(testEmail)

        val state = viewModel.uiState.value
        assertEquals(testEmail, state.email)
        assertNull(state.emailErrorRes)
        assertNull(state.generalErrorRes)
    }

    @Test
    fun `onPasswordChange updates password in uiState and clears errors`() = runTest {
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange("")
        viewModel.onConfirmPasswordChange("")
        viewModel.onSignUpClick()
        assertNotNull(viewModel.uiState.value.passwordErrorRes)

        viewModel.onPasswordChange(testPassword)

        val state = viewModel.uiState.value
        assertEquals(testPassword, state.password)
        assertNull(state.passwordErrorRes)
    }

    @Test
    fun `onConfirmPasswordChange updates confirmPassword in uiState and clears errors`() = runTest {
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword + "diff")
        viewModel.onSignUpClick()
        assertNotNull(viewModel.uiState.value.confirmPasswordErrorRes)

        viewModel.onConfirmPasswordChange(testPassword)

        val state = viewModel.uiState.value
        assertEquals(testPassword, state.confirmPassword)
        assertNull(state.confirmPasswordErrorRes)
    }

    // --- Validation Tests ---

    @Test
    fun `onSignUpClick with blank email sets emailErrorRes`() = runTest {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword)
        viewModel.onSignUpClick()

        assertEquals(R.string.error_email_empty, viewModel.uiState.value.emailErrorRes)
        // UseCase should not be called
        coVerify(exactly = 0) { mockSignUpUseCase(any(), any(), any()) }
    }

    @Test
    fun `onSignUpClick with invalid email format sets emailErrorRes`() = runTest {
        viewModel.onEmailChange("invalidemail")
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword)
        viewModel.onSignUpClick()

        assertEquals(R.string.error_email_invalid, viewModel.uiState.value.emailErrorRes)
        coVerify(exactly = 0) { mockSignUpUseCase(any(), any(), any()) }
    }

    @Test
    fun `onSignUpClick with blank password sets passwordErrorRes`() = runTest {
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange("")
        viewModel.onConfirmPasswordChange("")
        viewModel.onSignUpClick()

        assertEquals(R.string.error_password_empty, viewModel.uiState.value.passwordErrorRes)
        coVerify(exactly = 0) { mockSignUpUseCase(any(), any(), any()) }
    }

    @Test
    fun `onSignUpClick with short password sets passwordErrorRes`() = runTest {
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange("123")
        viewModel.onConfirmPasswordChange("123")
        viewModel.onSignUpClick()

        assertEquals(R.string.error_password_short, viewModel.uiState.value.passwordErrorRes)
        coVerify(exactly = 0) { mockSignUpUseCase(any(), any(), any()) }
    }

    @Test
    fun `onSignUpClick with mismatched passwords sets confirmPasswordErrorRes`() = runTest {
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange("differentPassword")
        viewModel.onSignUpClick()

        assertEquals(R.string.error_password_mismatch, viewModel.uiState.value.confirmPasswordErrorRes)
        coVerify(exactly = 0) { mockSignUpUseCase(any(), any(), any()) }
    }

    // --- UseCase Interaction Tests ---

    @Test
    fun `onSignUpClick with valid input and successful signUp emits NavigateTo(Schedule)`() = runTest {
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword)

        // Ensure we are passing "owner" role
        coEvery { mockSignUpUseCase(testEmail, testPassword, "owner") } returns Result.Success(successUser)

        viewModel.eventFlow.test(timeout = 3.seconds) {
            viewModel.onSignUpClick()

            val expectedEvent = SignUpViewEvent.NavigateTo(ScreenRoutes.Schedule.route)
            assertEquals(expectedEvent, awaitItem())

            cancelAndConsumeRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.generalErrorRes)

        coVerify(exactly = 1) { mockSignUpUseCase(testEmail, testPassword, "owner") }
    }

    @Test
    fun `onSignUpClick with valid input and failed signUp (UserCollision) sets correct generalErrorRes`() = runTest {
        coEvery { mockSignUpUseCase(testEmail, testPassword, "owner") } returns Result.Error(mockUserCollisionException)

        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword)
        viewModel.onSignUpClick()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(R.string.error_auth_email_collision, state.generalErrorRes)
        coVerify(exactly = 1) { mockSignUpUseCase(testEmail, testPassword, "owner") }
    }

    @Test
    fun `onSignUpClick with valid input and failed signUp (WeakPassword) sets correct generalErrorRes`() = runTest {
        coEvery { mockSignUpUseCase(testEmail, testPassword, "owner") } returns Result.Error(mockWeakPasswordException)

        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword)
        viewModel.onSignUpClick()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(R.string.error_auth_weak_password, state.generalErrorRes)
        coVerify(exactly = 1) { mockSignUpUseCase(testEmail, testPassword, "owner") }
    }

    @Test
    fun `onSignUpClick with valid input and failed signUp (OtherError) sets general signup_failed error`() = runTest {
        coEvery { mockSignUpUseCase(testEmail, testPassword, "owner") } returns Result.Error(mockOtherException)

        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword)
        viewModel.onSignUpClick()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(R.string.error_signup_failed, state.generalErrorRes)
        coVerify(exactly = 1) { mockSignUpUseCase(testEmail, testPassword, "owner") }
    }

    @Test
    fun `onSignUpClick while already loading does not call signUpUseCase again`() = runTest {
        // 1. ARRANGE
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword)

        // Mock delay to catch the loading state
        coEvery { mockSignUpUseCase(testEmail, testPassword, "owner") } coAnswers {
            delay(1000)
            Result.Success(successUser)
        }

        // 2. ACT - First Click
        viewModel.onSignUpClick()

        // CRITICAL: Advance until the delay
        runCurrent()

        assertTrue("UseCase should be running, so isLoading must be true", viewModel.uiState.value.isLoading)

        // Second Click (Should be ignored)
        viewModel.onSignUpClick()

        // Finish coroutines
        advanceUntilIdle()

        // 3. ASSERT - Verify exactly 1 call
        coVerify(exactly = 1) { mockSignUpUseCase(testEmail, testPassword, "owner") }

        assertFalse(viewModel.uiState.value.isLoading)
    }
}