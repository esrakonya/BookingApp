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

class GetCurrentUserUseCaseTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockAuthRepository: AuthRepository

    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    @Before
    fun setUp() {
        getCurrentUserUseCase = GetCurrentUserUseCase(mockAuthRepository)
    }

    @Test
    fun `invoke when user exists returns success with user`() = runTest {
        val expectedUser = AuthUser("uid123", "test@example.com")
        coEvery { mockAuthRepository.getCurrentUser() } returns Result.Success(expectedUser)

        val result = getCurrentUserUseCase()

        assertTrue("Result should be an instance of Result.Success", result is Result.Success)
        val actualUser = (result as Result.Success).data
        assertEquals(expectedUser, actualUser)
        coVerify(exactly = 1) { mockAuthRepository.getCurrentUser() }
    }

    @Test
    fun `invoke when no user exists returns success with null`() = runTest {
        coEvery { mockAuthRepository.getCurrentUser() } returns Result.Success(null)

        val result = getCurrentUserUseCase()

        assertTrue("Result should be an instance of Result.Success", result is Result.Success)
        val data = (result as Result.Success).data
        assertNull("Data inside Success should be null", data)
        coVerify(exactly = 1) { mockAuthRepository.getCurrentUser() }
    }

    @Test
    fun `invoke when repository fails returns failure`() = runTest {
        val exception = Exception("Repository error")
        coEvery { mockAuthRepository.getCurrentUser() } returns Result.Error(exception)

        val result = getCurrentUserUseCase()

        assertTrue("Result should be an instance of Result.Error", result is Result.Error)
        val actualException = (result as Result.Error).exception
        assertEquals(exception, actualException)
        coVerify(exactly = 1) { mockAuthRepository.getCurrentUser() }
    }
}