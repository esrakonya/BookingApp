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

class SignUpUseCaseTest {

    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var signUpUseCase: SignUpUseCase

    private val testEmail = "newuser@example.com"
    private val testPassword = "newpassword123"

    @Before
    fun setUp() {
        mockAuthRepository = mockk()
        signUpUseCase = SignUpUseCase(mockAuthRepository)
    }

    @Test
    fun `invoke with valid credentials calls repository and returns success`() = runTest {
        val expectedUser = AuthUser("uid456", testEmail)
        coEvery { mockAuthRepository.signUpWithEmailPassword(testEmail, testPassword) } returns Result.success(expectedUser)

        val result = signUpUseCase(testEmail, testPassword)

        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())
        coVerify(exactly = 1) { mockAuthRepository.signUpWithEmailPassword(testEmail, testPassword) }
    }

    @Test
    fun `invoke when repository fails returns failure`() = runTest {
        val exception = Exception("Firebase Create User Error")
        coEvery { mockAuthRepository.signUpWithEmailPassword(testEmail, testPassword) } returns Result.failure(exception)

        val result = signUpUseCase(testEmail, testPassword)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { mockAuthRepository.signUpWithEmailPassword(testEmail, testPassword) }
    }

    @Test
    fun `invoke with blank email returns failure without calling repository`() = runTest {
        val result = signUpUseCase("", testPassword)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Email or password cannot be blank.", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { mockAuthRepository.signUpWithEmailPassword(any(), any()) }
    }

    @Test
    fun `invoke with blank password returns failure without calling repository`() = runTest {
        val result = signUpUseCase(testEmail, "")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Email or password cannot be blank.", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { mockAuthRepository.signUpWithEmailPassword(any(), any()) }
    }
}