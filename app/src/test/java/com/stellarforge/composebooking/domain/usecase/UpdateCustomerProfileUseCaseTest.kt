package com.stellarforge.composebooking.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.domain.repository.CustomerProfileRepository
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [UpdateCustomerProfileUseCase].
 * Verifies business logic validation before calling the repository.
 */
class UpdateCustomerProfileUseCaseTest {

    // Mocking the dependency
    private val customerProfileRepository: CustomerProfileRepository = mockk(relaxed = true)
    private lateinit var useCase: UpdateCustomerProfileUseCase

    @Before
    fun setUp() {
        useCase = UpdateCustomerProfileUseCase(customerProfileRepository)
    }

    @Test
    fun `invoke should return Error when name is blank`() = runBlocking {
        // Arrange
        val userId = "user123"
        val invalidName = "   "
        val validPhone = "5551234567"

        // Act
        val result = useCase(userId, invalidName, validPhone)

        // Assert
        assertThat(result).isInstanceOf(Result.Error::class.java)

        // Ensure repository was NOT called
        coVerify(exactly = 0) { customerProfileRepository.updateCustomerProfile(any(), any(), any()) }
    }

    @Test
    fun `invoke should return Success and call UserRepo when inputs are valid`() = runBlocking {
        // Arrange
        val userId = "user123"
        val nameInput = " John Doe " // Input has spaces (needs trimming)
        val phoneInput = " 555-000 " // Input has spaces (needs trimming)

        // FIX IS HERE:
        // We tell MockK: "Return Success for ANY arguments."
        // This prevents the "Result(child of...)" error by ensuring the mock always works.
        coEvery {
            customerProfileRepository.updateCustomerProfile(any(), any(), any())
        } returns Result.Success(Unit)

        // Act
        val result = useCase(userId, nameInput, phoneInput)

        // Assert 1: Did the UseCase return Success?
        assertThat(result).isInstanceOf(Result.Success::class.java)

        // Assert 2 (CRITICAL CHECK): Did the UseCase call the Repository with TRIMMED values?
        // We verify the logic here.
        coVerify(exactly = 1) {
            customerProfileRepository.updateCustomerProfile(
                userId = userId,
                name = "John Doe", // We expect the trimmed version here
                phone = "555-000"  // We expect the trimmed version here
            )
        }
    }
}