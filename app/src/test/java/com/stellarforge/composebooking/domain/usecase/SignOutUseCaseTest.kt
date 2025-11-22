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
    fun `invoke calls repository and returns success when repository succeeds`() = runTest {
        coEvery { mockAuthRepository.signOut() } returns Result.Success(Unit)

        val result = signOutUseCase()

        assertTrue("Result should be an instance of Result.Success", result is Result.Success)
        coVerify(exactly = 1) { mockAuthRepository.signOut() }
    }

    @Test
    fun `invoke calls repository and returns failure when repository fails`() = runTest {
        // ARRANGE
        val expectedException = Exception("Sign out error")
        coEvery { mockAuthRepository.signOut() } returns Result.Error(expectedException)

        // ACT
        val result = signOutUseCase()

        // ASSERT
        // DÜZELTİLDİ:
        assertTrue("Result should be an instance of Result.Error", result is Result.Error)
        val errorResult = result as Result.Error

        // Exception türünü kontrol etmeye gerek yok, çünkü tam olarak hangi exception'ı beklediğimizi biliyoruz.
        // Doğrudan exception nesnelerini karşılaştıralım.
        assertEquals(expectedException, errorResult.exception)

        coVerify(exactly = 1) { mockAuthRepository.signOut() }
    }
}
