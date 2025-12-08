package com.stellarforge.composebooking.ui.screens.auth

import app.cash.turbine.test
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.domain.usecase.SignInUseCase
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.*
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
 * Unit tests for [OwnerLoginViewModel].
 *
 * Verifies the authentication logic specifically for Business Owners.
 * Ensures that Customers cannot access the owner dashboard and handles all error states.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnerLoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockSignInUseCase: SignInUseCase
    private lateinit var mockSignOutUseCase: SignOutUseCase

    private lateinit var viewModel: OwnerLoginViewModel

    private val testEmail = "owner@example.com"
    private val testPassword = "password123"

    private val successOwnerUser = AuthUser("uid_owner", testEmail, role = "owner")
    private val successCustomerUser = AuthUser("uid_cust", testEmail, role = "customer")

    private val mockNetworkException: FirebaseNetworkException = mockk()
    private val mockInvalidCredentialsException: FirebaseAuthInvalidCredentialsException = mockk()
    private val mockInvalidUserException: FirebaseAuthInvalidUserException = mockk()
    private val mockOtherException: Exception = mockk()

    @Before
    fun setUp() {
        mockSignInUseCase = mockk()
        mockSignOutUseCase = mockk {
            coEvery { this@mockk.invoke() } returns Result.Success(Unit)
        }

        viewModel = OwnerLoginViewModel(mockSignInUseCase, mockSignOutUseCase)

        every { mockNetworkException.localizedMessage } returns "Mocked Network Error"
        every { mockInvalidCredentialsException.localizedMessage } returns "Mocked Invalid Credentials"
        every { mockInvalidUserException.localizedMessage } returns "Mocked Invalid User"
        every { mockOtherException.localizedMessage } returns "Some other unknown error"
    }

    @Test
    fun `onLoginClick with valid input and successful OWNER signIn emits NavigateTo Schedule`() = runTest {
        // GIVEN
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)

        coEvery { mockSignInUseCase(testEmail, testPassword) } returns Result.Success(successOwnerUser)

        // WHEN & THEN
        viewModel.eventFlow.test(timeout = 3.seconds) {
            viewModel.onLoginClick()

            val expectedEvent = LoginViewEvent.NavigateTo(ScreenRoutes.Schedule.route)
            assertEquals(expectedEvent, awaitItem())

            cancelAndConsumeRemainingEvents()
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        coVerify(exactly = 1) { mockSignInUseCase(testEmail, testPassword) }
    }


    @Test
    fun `onLoginClick with valid input BUT wrong role (Customer) shows specific error and signs out`() = runTest {
        // GIVEN
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)
        coEvery { mockSignInUseCase(testEmail, testPassword) } returns Result.Success(successCustomerUser)

        // WHEN & THEN
        viewModel.eventFlow.test {
            viewModel.onLoginClick()

            // Expect Security Warning
            val expectedEvent = LoginViewEvent.ShowSnackbar(R.string.error_auth_customer_at_owner_login)
            assertEquals(expectedEvent, awaitItem())

            cancelAndConsumeRemainingEvents()
        }

        // Verify session is cleared
        coVerify(exactly = 1) { mockSignOutUseCase() }
    }


    @Test
    fun `onLoginClick with invalid email format sets emailErrorRes and DOES NOT call useCase`() = runTest {
        viewModel.onEmailChange("invalid-email-format")
        viewModel.onPasswordChange(testPassword)

        viewModel.onLoginClick()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_email_invalid, state.emailErrorRes)

        coVerify(exactly = 0) { mockSignInUseCase(any(), any()) }
    }

    @Test
    fun `onLoginClick with empty password sets passwordErrorRes`() = runTest {
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange("")

        viewModel.onLoginClick()

        val state = viewModel.uiState.value
        assertEquals(R.string.error_password_empty, state.passwordErrorRes)

        coVerify(exactly = 0) { mockSignInUseCase(any(), any()) }
    }

    @Test
    fun `onEmailChange clears previous error states`() = runTest {
        // GIVEN
        viewModel.onEmailChange("")
        viewModel.onLoginClick()
        assertNotNull(viewModel.uiState.value.emailErrorRes)

        // WHEN
        viewModel.onEmailChange("o")

        // THEN
        assertNull(viewModel.uiState.value.emailErrorRes)
        assertNull(viewModel.uiState.value.generalErrorRes)
    }


    @Test
    fun `onLoginClick with failed signIn (InvalidCredentials) sets correct generalErrorRes`() = runTest {
        coEvery { mockSignInUseCase(testEmail, testPassword) } returns Result.Error(mockInvalidCredentialsException)

        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)

        viewModel.onLoginClick()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(R.string.error_auth_invalid_credentials, state.generalErrorRes)
    }

    @Test
    fun `onLoginClick with failed signIn (NetworkError) sets correct generalErrorRes`() = runTest {
        coEvery { mockSignInUseCase(testEmail, testPassword) } returns Result.Error(mockNetworkException)

        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)

        viewModel.onLoginClick()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(R.string.error_network_connection, state.generalErrorRes)
    }


    @Test
    fun `onLoginClick while already loading does not call signInUseCase again`() = runTest {
        // ARRANGE
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)

        coEvery { mockSignInUseCase(testEmail, testPassword) } coAnswers {
            delay(1000)
            Result.Success(successOwnerUser)
        }

        // ACT 1
        viewModel.onLoginClick()
        runCurrent()

        assertTrue(viewModel.uiState.value.isLoading)

        // ACT 2
        viewModel.onLoginClick()
        advanceUntilIdle()

        // ASSERT
        coVerify(exactly = 1) { mockSignInUseCase(testEmail, testPassword) }
        assertFalse(viewModel.uiState.value.isLoading)
    }
}