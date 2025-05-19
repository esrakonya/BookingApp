package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SignOutUseCaseTest {

    private lateinit var mockAuthRepository: AuthRepository
    private lateinit var signOutUseCase: SignOutUseCase

    @Before
    fun setUp() {
        mockAuthRepository = mockk()
        signOutUseCase = SignOutUseCase(mockAuthRepository)
    }

    @Test
    fun `invoke calls repository and returns success when repository succeeds`() = runTest {
        coEvery { mockAuthRepository.signOut() } returns Result.success(Unit)

        val result = signOutUseCase()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { mockAuthRepository.signOut() }
    }

    @Test
    fun `invoke calls repository and returns failure when repository fails`() = runTest {
        val exception = Exception("Sign out error")
        coEvery { mockAuthRepository.signOut() } returns Result.failure(exception)

        val result = signOutUseCase()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { mockAuthRepository.signOut() }
    }
}
