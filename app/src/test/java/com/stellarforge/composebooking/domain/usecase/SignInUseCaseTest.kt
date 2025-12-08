package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.domain.repository.AuthRepository
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [SignInUseCase].
 * Validates authentication flow and input constraints.
 */
class SignInUseCaseTest {

    @get:Rule
    val mockKRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockAuthRepository: AuthRepository

    private lateinit var signInUseCase: SignInUseCase

    private val testEmail = "test@example.com"
    private val testPassword = "password123"

    @Before
    fun setUp() {
        signInUseCase = SignInUseCase(mockAuthRepository)
    }

    @Test
    fun `invoke with valid credentials calls repository and returns success`() = runTest {
        // ARRANGE
        val expectedUser = AuthUser("uid123", testEmail)
        coEvery { mockAuthRepository.signInWithEmailPassword(testEmail, testPassword) } returns Result.Success(expectedUser)

        // ACT
        val result = signInUseCase(testEmail, testPassword)

        // ASSERT
        assertTrue("Result should be an instance of Result.Success", result is Result.Success)
        val actualUser = (result as Result.Success).data
        assertEquals(expectedUser, actualUser)

        coVerify(exactly = 1) { mockAuthRepository.signInWithEmailPassword(testEmail, testPassword) }
    }

    @Test
    fun `invoke when repository fails returns failure`() = runTest {
        // ARRANGE
        val exception = Exception("Firebase Auth Error")
        coEvery { mockAuthRepository.signInWithEmailPassword(testEmail, testPassword) } returns Result.Error(exception)

        // ACT
        val result = signInUseCase(testEmail, testPassword)

        // ASSERT
        assertTrue("Result should be an instance of Result.Error", result is Result.Error)
        val actualException = (result as Result.Error).exception
        assertEquals(exception, actualException)

        coVerify(exactly = 1) { mockAuthRepository.signInWithEmailPassword(testEmail, testPassword) }
    }

    @Test
    fun `invoke with blank email returns validation error`() = runTest {
        // ACT
        val result = signInUseCase(" ", testPassword)

        // ASSERT
        assertTrue("Result should be an instance of Result.Error", result is Result.Error)
        val errorResult = result as Result.Error
        assertTrue("Exception should be IllegalArgumentException", errorResult.exception is IllegalArgumentException)
        assertEquals("Email and password cannot be blank.", errorResult.exception.message)

        coVerify(exactly = 0) { mockAuthRepository.signInWithEmailPassword(any(), any()) }
    }

    @Test
    fun `invoke with blank password returns validation error`() = runTest {
        // ACT
        val result = signInUseCase(testEmail, "  ")

        // ASSERT
        assertTrue("Result should be an instance of Result.Error", result is Result.Error)
        val errorResult = result as Result.Error
        assertTrue("Exception should be IllegalArgumentException", errorResult.exception is IllegalArgumentException)

        coVerify(exactly = 0) { mockAuthRepository.signInWithEmailPassword(any(), any()) }
    }
}