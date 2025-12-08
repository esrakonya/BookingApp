package com.stellarforge.composebooking.domain.usecase

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
 * Unit tests for [SignOutUseCase].
 */
class SignOutUseCaseTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockAuthRepository: AuthRepository

    private lateinit var signOutUseCase: SignOutUseCase

    @Before
    fun setUp() {
        signOutUseCase = SignOutUseCase(mockAuthRepository)
    }

    @Test
    fun `invoke calls repository and returns success`() = runTest {
        // ARRANGE
        coEvery { mockAuthRepository.signOut() } returns Result.Success(Unit)

        // ACT
        val result = signOutUseCase()

        // ASSERT
        assertTrue("Result should be an instance of Result.Success", result is Result.Success)
        coVerify(exactly = 1) { mockAuthRepository.signOut() }
    }

    @Test
    fun `invoke calls repository and returns failure on error`() = runTest {
        // ARRANGE
        val expectedException = Exception("Sign out error")
        coEvery { mockAuthRepository.signOut() } returns Result.Error(expectedException)

        // ACT
        val result = signOutUseCase()

        // ASSERT
        assertTrue("Result should be an instance of Result.Error", result is Result.Error)
        val errorResult = result as Result.Error
        assertEquals(expectedException, errorResult.exception)

        coVerify(exactly = 1) { mockAuthRepository.signOut() }
    }
}