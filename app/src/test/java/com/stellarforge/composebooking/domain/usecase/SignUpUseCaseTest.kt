package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.repository.AuthRepository
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SignUpUseCaseTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockAuthRepository: AuthRepository

    private lateinit var signUpUseCase: SignUpUseCase

    private val testEmail = "newuser@example.com"
    private val testPassword = "newpassword123"

    @Before
    fun setUp() {
        signUpUseCase = SignUpUseCase(mockAuthRepository)
    }

    @Test
    fun `invoke with valid credentials calls repository and returns success`() = runTest {
        val expectedUser = AuthUser("uid456", testEmail)
        coEvery { mockAuthRepository.signUpWithEmailPassword(testEmail, testPassword) } returns Result.Success(expectedUser)

        val result = signUpUseCase(testEmail, testPassword)

        assertTrue("Result should be an instance of Result.Success", result is Result.Success)
        val actualUser = (result as Result.Success).data
        assertEquals(expectedUser, actualUser)

        coVerify(exactly = 1) { mockAuthRepository.signUpWithEmailPassword(testEmail, testPassword) }
    }

    @Test
    fun `invoke when repository fails returns failure`() = runTest {
        val exception = Exception("Firebase Create User Error")
        coEvery { mockAuthRepository.signUpWithEmailPassword(testEmail, testPassword) } returns Result.Error(exception)

        val result = signUpUseCase(testEmail, testPassword)

        assertTrue("Result should be an instance of Result.Error", result is Result.Error)
        val actualException = (result as Result.Error).exception
        assertEquals(exception, actualException)

        coVerify(exactly = 1) { mockAuthRepository.signUpWithEmailPassword(testEmail, testPassword) }
    }

    @Test
    fun `invoke with blank email returns failure without calling repository`() = runTest {
        val result = signUpUseCase("", testPassword)

        assertTrue("Result should be an instance of Result.Error", result is Result.Error)
        val errorResult = result as Result.Error
        assertTrue("Exception should be IllegalArgumentException", errorResult.exception is IllegalArgumentException)
        assertEquals("Email or password cannot be blank.", errorResult.exception.message)

        coVerify(exactly = 0) { mockAuthRepository.signUpWithEmailPassword(any(), any()) }
    }

    @Test
    fun `invoke with blank password returns failure without calling repository`() = runTest {
        val result = signUpUseCase(testEmail, "")

        assertTrue("Result should be an instance of Result.Error", result is Result.Error)
        val errorResult = result as Result.Error
        assertTrue("Exception should be IllegalArgumentException", errorResult.exception is IllegalArgumentException)
        assertEquals("Email or password cannot be blank.", errorResult.exception.message)

        coVerify(exactly = 0) { mockAuthRepository.signUpWithEmailPassword(any(), any()) }
    }
}