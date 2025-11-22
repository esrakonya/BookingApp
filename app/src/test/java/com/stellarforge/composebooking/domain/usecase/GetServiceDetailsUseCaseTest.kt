package com.stellarforge.composebooking.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
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

class GetServiceDetailsUseCaseTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockRepository: AppointmentRepository


     private lateinit var getServiceDetailsUseCase: GetServiceDetailsUseCase

     @Before
     fun setUp() {
         // Use case'i mock repository ile oluştur
         getServiceDetailsUseCase = GetServiceDetailsUseCase(mockRepository)
     }

    @Test
    fun `invoke - with valid serviceId - when repository returns success - should return success with service`() = runTest {
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

        val actualResult = getServiceDetailsUseCase(fakeServiceId)

        assertThat(actualResult).isInstanceOf(Result.Success::class.java)
        val serviceData = (actualResult as Result.Success).data
        assertThat(serviceData).isNotNull()
        assertThat(serviceData).isEqualTo(expectedService)

        coVerify(exactly = 1) { mockRepository.getServiceDetails(fakeServiceId) }
    }

    @Test
    fun `invoke - with a non-existent serviceId - when repository returns DocumentNotFoundException - should return Success with null data`() = runTest {
        val fakeServiceId = "non-existent-id"
        coEvery { mockRepository.getServiceDetails(fakeServiceId) } returns Result.Error(DocumentNotFoundException("Not Found"))

        val actualResult = getServiceDetailsUseCase(fakeServiceId)

        assertThat(actualResult).isInstanceOf(Result.Success::class.java)
        val serviceData = (actualResult as Result.Success).data
        assertThat(serviceData).isNull()

        coVerify(exactly = 1) { mockRepository.getServiceDetails(fakeServiceId) }
    }

    @Test
    fun `invoke - when repository returns a general error - should pass the error through`() = runTest {
        // ARRANGE: Repository'nin genel bir veritabanı hatası döndüreceğini ayarla
        val fakeServiceId = "test-id-error"
        val genericException = Exception("Firestore connection failed")
        coEvery { mockRepository.getServiceDetails(fakeServiceId) } returns Result.Error(genericException)

        // ACT: UseCase'i çalıştır
        val actualResult = getServiceDetailsUseCase(fakeServiceId)

        // ASSERT: UseCase'in, "bulunamadı" DIŞINDAKİ hataları değiştirmeden, olduğu gibi ilettiğini doğrula
        assertThat(actualResult).isInstanceOf(Result.Error::class.java)
        val actualException = (actualResult as Result.Error).exception
        assertThat(actualException).isEqualTo(genericException)

        coVerify(exactly = 1) { mockRepository.getServiceDetails(fakeServiceId) }
    }

    @Test
    fun `invoke - with a blank serviceId - should return illegal argument error without calling repository`() = runTest {
        // ARRANGE: Geçersiz (boş) bir ID hazırla
        val blankServiceId = " "

        // ACT: UseCase'i geçersiz ID ile çalıştır
        val actualResult = getServiceDetailsUseCase(blankServiceId)

        // ASSERT: Sonucun beklenen validasyon hatası olduğunu doğrula
        assertThat(actualResult).isInstanceOf(Result.Error::class.java)
        val exception = (actualResult as Result.Error).exception
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception.message).isEqualTo("Service ID cannot be blank.")

        // Repository'nin HİÇ çağrılmadığını doğrula
        coVerify(exactly = 0) { mockRepository.getServiceDetails(any()) }
    }
}