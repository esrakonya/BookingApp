package com.stellarforge.composebooking.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [AddServiceUseCase].
 * Verifies that services are correctly passed to the repository with the necessary ownership details.
 */
class AddServiceUseCaseTest {
    private val mockRepo = mockk<ServiceRepository>()
    private val useCase = AddServiceUseCase(mockRepo)

    @Test
    fun `invoke - calls repository addService successfully`() = runTest {
        // ARRANGE
        // Note: 'ownerId' is crucial for multi-tenancy support in the backend.
        val service = Service(name = "New Service", ownerId = "test_owner_123")
        coEvery { mockRepo.addService(service) } returns Result.Success(Unit)

        // ACT
        val result = useCase(service)

        // ASSERT
        assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify(exactly = 1) { mockRepo.addService(service) }
    }
}