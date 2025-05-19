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

class GetCurrentUserUseCaseTest {

    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    @Before
    fun setUp() {
        mockAuthRepository = mockk()
        getCurrentUserUseCase = GetCurrentUserUseCase(mockAuthRepository)
    }

    @Test
    fun `invoke when user exists returns success with user`() = runTest {
        val expectedUser = AuthUser("uid123", "test@example.com")
        coEvery { mockAuthRepository.getCurrentUser() } returns Result.success(expectedUser)

        val result = getCurrentUserUseCase()

        assertTrue(result.isSuccess)
        assertEquals(expectedUser, result.getOrNull())
        coVerify(exactly = 1) { mockAuthRepository.getCurrentUser() }
    }

    @Test
    fun `invoke when no user exists returns success with null`() = runTest {
        coEvery { mockAuthRepository.getCurrentUser() } returns Result.success(null)

        val result = getCurrentUserUseCase()

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        coVerify(exactly = 1) { mockAuthRepository.getCurrentUser() }
    }

    @Test
    fun `invoke when repository fails returns failure`() = runTest {
        val exception = Exception("Repository error")
        coEvery { mockAuthRepository.getCurrentUser() } returns Result.failure(exception)

        val result = getCurrentUserUseCase()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { mockAuthRepository.getCurrentUser() }
    }
}