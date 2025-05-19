package com.stellarforge.composebooking.ui.screens.auth

import app.cash.turbine.test
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.domain.usecase.SignUpUseCase
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.google.firebase.FirebaseNetworkException // Sadece TİPİNİ belirtmek için import
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockSignUpUseCase: SignUpUseCase
    private lateinit var viewModel: SignUpViewModel

    private val testEmail = "newuser@example.com"
    private val testPassword = "password123"
    private val successUser = AuthUser("uid789", testEmail)

    // Mock exception'lar
    private val mockNetworkException: FirebaseNetworkException = mockk()
    private val mockUserCollisionException: FirebaseAuthUserCollisionException = mockk()
    private val mockWeakPasswordException: FirebaseAuthWeakPasswordException = mockk()
    private val mockOtherException: Exception = mockk()

    @Before
    fun setUp() {
        mockSignUpUseCase = mockk()
        viewModel = SignUpViewModel(mockSignUpUseCase)

        // Varsayılan başarılı signUp
        coEvery { mockSignUpUseCase(any(), any()) } returns Result.success(successUser)

        // Mock exception'lar için temel davranışlar
        every { mockNetworkException.localizedMessage } returns "Mocked Network Error"
        every { mockUserCollisionException.localizedMessage } returns "Mocked Email Collision"
        every { mockWeakPasswordException.localizedMessage } returns "Mocked Weak Password"
        every { mockOtherException.localizedMessage } returns "Some other unknown error"
    }

    @Test
    fun `onEmailChange updates email in uiState and clears errors`() = runTest {
        viewModel.onEmailChange("")
        viewModel.onSignUpClick() // Hata tetikle
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
        viewModel.onConfirmPasswordChange("") // Confirm password'ı da boş bırakalım
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
        viewModel.onConfirmPasswordChange(testPassword + "diff") // Farklı şifre ile hata tetikle
        viewModel.onSignUpClick()
        assertNotNull(viewModel.uiState.value.confirmPasswordErrorRes)

        viewModel.onConfirmPasswordChange(testPassword)

        val state = viewModel.uiState.value
        assertEquals(testPassword, state.confirmPassword)
        assertNull(state.confirmPasswordErrorRes)
    }

    // --- Validasyon Testleri ---
    @Test
    fun `onSignUpClick with blank email sets emailErrorRes`() = runTest {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword)
        viewModel.onSignUpClick()
        assertEquals(R.string.error_email_empty, viewModel.uiState.value.emailErrorRes)
        coVerify(exactly = 0) { mockSignUpUseCase(any(), any()) }
    }

    @Test
    fun `onSignUpClick with invalid email format sets emailErrorRes`() = runTest {
        viewModel.onEmailChange("invalidemail")
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword)
        viewModel.onSignUpClick()
        assertEquals(R.string.error_email_invalid, viewModel.uiState.value.emailErrorRes)
        coVerify(exactly = 0) { mockSignUpUseCase(any(), any()) }
    }

    @Test
    fun `onSignUpClick with blank password sets passwordErrorRes`() = runTest {
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange("")
        viewModel.onConfirmPasswordChange("")
        viewModel.onSignUpClick()
        assertEquals(R.string.error_password_empty, viewModel.uiState.value.passwordErrorRes)
        coVerify(exactly = 0) { mockSignUpUseCase(any(), any()) }
    }

    @Test
    fun `onSignUpClick with short password sets passwordErrorRes`() = runTest {
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange("123") // Kısa şifre
        viewModel.onConfirmPasswordChange("123")
        viewModel.onSignUpClick()
        assertEquals(R.string.error_password_short, viewModel.uiState.value.passwordErrorRes)
        coVerify(exactly = 0) { mockSignUpUseCase(any(), any()) }
    }

    @Test
    fun `onSignUpClick with mismatched passwords sets confirmPasswordErrorRes`() = runTest {
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange("differentPassword")
        viewModel.onSignUpClick()
        assertEquals(R.string.error_password_mismatch, viewModel.uiState.value.confirmPasswordErrorRes)
        coVerify(exactly = 0) { mockSignUpUseCase(any(), any()) }
    }

    // --- UseCase Çağrı Testleri ---
    @Test
    fun `onSignUpClick with valid input and successful signUp emits NavigateToServiceList`() = runTest {
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword) // Eşleşen şifreler

        // UseCase'in başarılı döneceği setUp'ta ayarlandı.

        viewModel.eventFlow.test(timeout = 3.seconds) {
            viewModel.onSignUpClick()
            assertEquals(SignUpViewEvent.NavigateToServiceList, awaitItem())
            cancelAndConsumeRemainingEvents()
        }

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.generalErrorRes)
        coVerify(exactly = 1) { mockSignUpUseCase(testEmail, testPassword) }
    }

    @Test
    fun `onSignUpClick with valid input and failed signUp (UserCollision) sets correct generalErrorRes`() = runTest {
        coEvery { mockSignUpUseCase(testEmail, testPassword) } returns Result.failure(mockUserCollisionException)

        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword)
        viewModel.onSignUpClick()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(R.string.error_auth_email_collision, state.generalErrorRes)
        coVerify(exactly = 1) { mockSignUpUseCase(testEmail, testPassword) }
    }

    @Test
    fun `onSignUpClick with valid input and failed signUp (WeakPassword) sets correct generalErrorRes`() = runTest {
        coEvery { mockSignUpUseCase(testEmail, testPassword) } returns Result.failure(mockWeakPasswordException)

        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword) // ViewModel validasyonu geçmeli, Firebase'den hata gelmeli
        viewModel.onConfirmPasswordChange(testPassword)
        viewModel.onSignUpClick()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(R.string.error_auth_weak_password, state.generalErrorRes)
        coVerify(exactly = 1) { mockSignUpUseCase(testEmail, testPassword) }
    }

    @Test
    fun `onSignUpClick with valid input and failed signUp (OtherError) sets general signup_failed error`() = runTest {
        coEvery { mockSignUpUseCase(testEmail, testPassword) } returns Result.failure(mockOtherException)

        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword)
        viewModel.onSignUpClick()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(R.string.error_signup_failed, state.generalErrorRes)
        coVerify(exactly = 1) { mockSignUpUseCase(testEmail, testPassword) }
    }

    @Test
    fun `onSignUpClick while already loading does not call signUpUseCase again`() = runTest {
        // Arrange
        viewModel.onEmailChange(testEmail)
        viewModel.onPasswordChange(testPassword)
        viewModel.onConfirmPasswordChange(testPassword) // Geçerli inputlar

        // UseCase'in ilk çağrıda bir süre beklemesini sağla ki isLoading true kalsın
        // ve ikinci onSignUpClick çağrıldığında isLoading hala true olsun.
        coEvery { mockSignUpUseCase(testEmail, testPassword) } coAnswers {
            // isLoading'in true olarak set edildiğini ve ikinci tıklamanın
            // UseCase'i tekrar tetiklemediğini doğrulamak için bir gecikme ekleyelim.
            delay(100.milliseconds) // veya daha uzun bir süre, dispatcher'a bağlı
            Result.success(successUser)
        }

        // Act
        viewModel.onSignUpClick() // İlk çağrı: isLoading true olur, UseCase başlar (ve delay sayesinde hemen bitmez)

        // İlk tıklamadan hemen sonra isLoading'in true olduğunu doğrula
        // runCurrent() veya advanceUntilIdle() ViewModel'daki launch bloğunun başlamasını sağlar
        runCurrent() // veya advanceTimeBy(1) // isLoading'in güncellenmesi için
        assertTrue("isLoading should be true after first click and before use case finishes", viewModel.uiState.value.isLoading)

        viewModel.onSignUpClick() // İkinci çağrı (isLoading hala true iken yapılmalı)

        // Assert
        // signUpUseCase'in sadece bir kez çağrıldığını doğrula
        coVerify(exactly = 1) { mockSignUpUseCase(testEmail, testPassword) }

        // Testin temizlenmesi ve tüm coroutine'lerin bitmesi için
        advanceUntilIdle() // Gecikmeli UseCase çağrısının tamamlanmasını bekle

        // Son durumu kontrol et (opsiyonel ama iyi bir pratik)
        val finalState = viewModel.uiState.value
        assertFalse("isLoading should be false after use case completes", finalState.isLoading)
        assertNull("generalErrorRes should be null on success", finalState.generalErrorRes)
    }
}