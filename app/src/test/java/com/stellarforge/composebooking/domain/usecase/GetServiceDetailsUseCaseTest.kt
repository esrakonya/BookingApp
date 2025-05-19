package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.data.repository.AppointmentRepository
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class GetServiceDetailsUseCaseTest {
     private lateinit var mockRepository: AppointmentRepository

     // Test edilecek sınıfın örneği
     private lateinit var getServiceDetailsUseCase: GetServiceDetailsUseCase

     // Her testten önce çalışacak kurulum fonksiyonu
     @Before
     fun setUp() {
         // Repository'i mockla
         mockRepository = mockk()

         // Use case'i mock repository ile oluştur
         getServiceDetailsUseCase = GetServiceDetailsUseCase(mockRepository)
     }

    @Test
    fun `invoke() when repository returns success should return success with service`() = runTest {
        val fakeServiceId = "test-id-123"
        val expectedService = Service(
            id = fakeServiceId,
            name = "Test Service",
            description = "Desc",
            durationMinutes = 60,
            price = 50.0,
            isActive = true
        )

        val successResult: Result<Service> = Result.success(expectedService)

        coEvery { mockRepository.getServiceDetails(fakeServiceId) } returns successResult

        val actualResult = getServiceDetailsUseCase(fakeServiceId)

        assertTrue(actualResult.isSuccess)
        assertEquals(expectedService, actualResult.getOrNull())
    }

    @Test
    fun `invoke() when repository returns failure should return failure`() = runTest {
        val fakeServiceId = "test-id-404"
        val expectedException = Exception("Service not found in mock")
        val failureResult: Result<Service> = Result.failure(expectedException)

        coEvery { mockRepository.getServiceDetails(fakeServiceId) } returns failureResult

        val actualResult = getServiceDetailsUseCase(fakeServiceId)

        assertTrue(actualResult.isFailure)
        assertEquals(expectedException, actualResult.exceptionOrNull())
    }

    @Test
    fun `invoke() when repository throws exception should return failure wrapping the exception`() = runTest {
        val fakeServiceId = "test-id-err"
        val expectedCause = RuntimeException("Firestore error")

        coEvery { mockRepository.getServiceDetails(fakeServiceId) } throws expectedCause

        val actualResult = getServiceDetailsUseCase(fakeServiceId)

        assertTrue("Result should be failure when repository throws", actualResult.isFailure)
        assertNotNull("Exception should not be null in failure result", actualResult.exceptionOrNull())
        assertTrue("Exception cause should be the expected RuntimeException", actualResult.exceptionOrNull() is RuntimeException)
        assertEquals(expectedCause.message, actualResult.exceptionOrNull()?.message)
    }
}