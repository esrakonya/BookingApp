package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SignInUseCaseTest {

    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var signInUseCase: SignInUseCase

    private val testEmail = "test@example.com"
    private val testPassword = "password123"

    @Before
    fun setUp() {
        mockAuthRepository = mockk()
        signInUseCase = SignInUseCase(mockAuthRepository)
    }

    @Test
    fun `invoke with valid credentials calls repository and returns success`() = runTest {
        val expectedUser = AuthUser("uid123", testEmail)
        coEvery { mockAuthRepository.signInWithEmailPassword(testEmail, testPassword) } returns Result.success(expectedUser)

        val result = signInUseCase(testEmail, testPassword)

        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())
        coVerify(exactly = 1) { mockAuthRepository.signInWithEmailPassword(testEmail, testPassword) }
    }

    @Test
    fun `invoke when repository fails returns failure`() = runTest {
        val exception = Exception("Firebase Auth Error")
        coEvery { mockAuthRepository.signInWithEmailPassword(testEmail, testPassword) } returns Result.failure(exception)

        val result = signInUseCase(testEmail, testPassword)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { mockAuthRepository.signInWithEmailPassword(testEmail, testPassword) }
    }

    @Test
    fun `invoke with blank email returns failure without calling repository`() = runTest {
        val result = signInUseCase(testEmail, " ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Email and password cannot be blank.", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { mockAuthRepository.signInWithEmailPassword(any(), any()) }
    }

    @Test
    fun `invoke with blank password returns failure without calling repository`() = runTest {
        val result = signInUseCase(testEmail, "  ") // Boşluklu boş şifre

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Email and password cannot be blank.", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { mockAuthRepository.signInWithEmailPassword(any(), any()) }
    }
}