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
 * Unit tests for [SignUpUseCase].
 * Handles user registration logic including role assignment (Customer vs Business Owner).
 */
class SignUpUseCaseTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockAuthRepository: AuthRepository

    private lateinit var signUpUseCase: SignUpUseCase

    private val testEmail = "newuser@example.com"
    private val testPassword = "newpassword123"
    private val testRole = "customer"

    @Before
    fun setUp() {
        signUpUseCase = SignUpUseCase(mockAuthRepository)
    }

    @Test
    fun `invoke with valid credentials calls repository and returns success`() = runTest {
        // ARRANGE
        val expectedUser = AuthUser("uid456", testEmail, role = testRole)
        coEvery {
            mockAuthRepository.signUpWithEmailPassword(testEmail, testPassword, testRole)
        } returns Result.Success(expectedUser)

        // ACT
        val result = signUpUseCase(testEmail, testPassword, testRole)

        // ASSERT
        assertTrue("Result should be an instance of Result.Success", result is Result.Success)
        val actualUser = (result as Result.Success).data
        assertEquals(expectedUser, actualUser)

        coVerify(exactly = 1) {
            mockAuthRepository.signUpWithEmailPassword(testEmail, testPassword, testRole)
        }
    }

    @Test
    fun `invoke when repository fails returns failure`() = runTest {
        // ARRANGE
        val exception = Exception("Firebase Create User Error")
        coEvery {
            mockAuthRepository.signUpWithEmailPassword(testEmail, testPassword, testRole)
        } returns Result.Error(exception)

        // ACT
        val result = signUpUseCase(testEmail, testPassword, testRole)

        // ASSERT
        assertTrue("Result should be an instance of Result.Error", result is Result.Error)
        val actualException = (result as Result.Error).exception
        assertEquals(exception, actualException)

        coVerify(exactly = 1) {
            mockAuthRepository.signUpWithEmailPassword(testEmail, testPassword, testRole)
        }
    }

    @Test
    fun `invoke with blank email returns validation error`() = runTest {
        // ACT
        val result = signUpUseCase("", testPassword, testRole)

        // ASSERT
        assertTrue("Result should be an instance of Result.Error", result is Result.Error)
        val errorResult = result as Result.Error
        assertTrue("Exception should be IllegalArgumentException", errorResult.exception is IllegalArgumentException)

        coVerify(exactly = 0) {
            mockAuthRepository.signUpWithEmailPassword(any(), any(), any())
        }
    }
}