package com.stellarforge.composebooking.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.utils.DocumentNotFoundException
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [GetServiceDetailsUseCase].
 * Verifies fetching service details and error handling logic.
 */
class GetServiceDetailsUseCaseTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockRepository: ServiceRepository

    private lateinit var getServiceDetailsUseCase: GetServiceDetailsUseCase

    @Before
    fun setUp() {
        getServiceDetailsUseCase = GetServiceDetailsUseCase(mockRepository)
    }

    @Test
    fun `invoke - with valid serviceId - when repository returns success - returns service data`() = runTest {
        // ARRANGE
        val fakeServiceId = "test-id-123"
        val expectedService = Service(
            id = fakeServiceId,
            ownerId = "owner-1",
            name = "Test Service",
            description = "Desc",
            durationMinutes = 60,
            priceInCents = 5000L,
            isActive = true
        )

        coEvery { mockRepository.getServiceDetails(fakeServiceId) } returns Result.Success(expectedService)

        // ACT
        val actualResult = getServiceDetailsUseCase(fakeServiceId)

        // ASSERT
        assertThat(actualResult).isInstanceOf(Result.Success::class.java)
        val serviceData = (actualResult as Result.Success).data
        assertThat(serviceData).isNotNull()
        assertThat(serviceData).isEqualTo(expectedService)

        coVerify(exactly = 1) { mockRepository.getServiceDetails(fakeServiceId) }
    }

    @Test
    fun `invoke - with non-existent serviceId - handles DocumentNotFoundException gracefully`() = runTest {
        // ARRANGE
        val fakeServiceId = "non-existent-id"
        // Simulate "Not Found" error from Repository
        coEvery { mockRepository.getServiceDetails(fakeServiceId) } returns Result.Error(DocumentNotFoundException("Not Found"))

        // ACT
        val actualResult = getServiceDetailsUseCase(fakeServiceId)

        // ASSERT
        // UseCase should swallow the specific exception and return Success(null) based on business requirements
        assertThat(actualResult).isInstanceOf(Result.Success::class.java)
        val serviceData = (actualResult as Result.Success).data
        assertThat(serviceData).isNull()

        coVerify(exactly = 1) { mockRepository.getServiceDetails(fakeServiceId) }
    }

    @Test
    fun `invoke - when repository returns a general error - propagates error`() = runTest {
        // ARRANGE
        val fakeServiceId = "test-id-error"
        val genericException = Exception("Firestore connection failed")
        coEvery { mockRepository.getServiceDetails(fakeServiceId) } returns Result.Error(genericException)

        // ACT
        val actualResult = getServiceDetailsUseCase(fakeServiceId)

        // ASSERT
        assertThat(actualResult).isInstanceOf(Result.Error::class.java)
        val actualException = (actualResult as Result.Error).exception
        assertThat(actualException).isEqualTo(genericException)

        coVerify(exactly = 1) { mockRepository.getServiceDetails(fakeServiceId) }
    }

    @Test
    fun `invoke - with a blank serviceId - returns validation error`() = runTest {
        // ACT
        val blankServiceId = " "
        val actualResult = getServiceDetailsUseCase(blankServiceId)

        // ASSERT
        assertThat(actualResult).isInstanceOf(Result.Error::class.java)
        val exception = (actualResult as Result.Error).exception
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)

        // Repository should NOT be called
        coVerify(exactly = 0) { mockRepository.getServiceDetails(any()) }
    }
}