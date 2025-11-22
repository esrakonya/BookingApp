package com.stellarforge.composebooking.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentReference
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.BookedSlot // Yeni import
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.domain.repository.SlotRepository // Yeni import
import com.stellarforge.composebooking.utils.Result
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CreateAppointmentUseCaseTest {

    // Mock Bağımlılıklar
    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockAppointmentRepository: AppointmentRepository

    @RelaxedMockK
    private lateinit var mockSlotRepository: SlotRepository

    @RelaxedMockK
    private lateinit var mockDocumentReference: DocumentReference

    // Test Edilecek Sınıf
    private lateinit var createAppointmentUseCase: CreateAppointmentUseCase

    // Test Verileri
    private val testOwnerId = "test-owner-id-456"
    private val testServicePriceInCents = 7500L
    private val testAppointmentId = "new-appt-id-123"
    private val testUserId = "user-test-123"
    private val testServiceId = "service-abc"
    private val testServiceName = "Test Service"
    private val testServiceDuration = 60
    private val testDate = LocalDate.of(2025, 8, 1)
    private val testTime = LocalTime.of(11, 0)
    private val testCustomerName = " Esra Test "
    private val testCustomerPhone = " 1234567890 "
    private val testCustomerEmail: String? = " test@example.com "
    private val testCustomerEmailEmpty: String? = "  "

    @Before
    fun setUp() {
        every { mockAppointmentRepository.getNewAppointmentReference() } returns mockDocumentReference
        every { mockDocumentReference.id } returns testAppointmentId
        coEvery { mockAppointmentRepository.createAppointmentWithId(any(), any()) } returns Result.Success(Unit)
        coEvery { mockSlotRepository.addSlot(any()) } returns Result.Success(Unit)

        createAppointmentUseCase = CreateAppointmentUseCase(
            mockAppointmentRepository,
            mockSlotRepository
        )
    }

    @Test
    fun `invoke with valid data calls repositories with correct data and returns success`() = runTest {
        // Arrange
        val appointmentSlot = slot<Appointment>()
        val slotSlot = slot<BookedSlot>()

        // Act
        val actualResult = createAppointmentUseCase(
            ownerId = testOwnerId,
            servicePriceInCents = testServicePriceInCents,
            userId = testUserId,
            serviceId = testServiceId,
            serviceName = testServiceName,
            serviceDuration = testServiceDuration,
            date = testDate,
            time = testTime,
            customerName = testCustomerName,
            customerPhone = testCustomerPhone,
            customerEmail = testCustomerEmail
        )

        assertThat(actualResult).isInstanceOf(Result.Success::class.java)

        // Verify - Doğru metotlar çağrıldı mı?
        coVerify(exactly = 1) { mockAppointmentRepository.createAppointmentWithId(mockDocumentReference, capture(appointmentSlot)) }
        coVerify(exactly = 1) { mockSlotRepository.addSlot(capture(slotSlot)) }

        // Assert - Yakalanan Appointment Doğru mu?
        val capturedAppointment = appointmentSlot.captured
        val expectedStartInstant = testDate.atTime(testTime).atZone(ZoneId.systemDefault()).toInstant()

        assertThat(capturedAppointment.id).isEqualTo(testAppointmentId)
        assertThat(capturedAppointment.ownerId).isEqualTo(testOwnerId)
        assertThat(capturedAppointment.userId).isEqualTo(testUserId)
        assertThat(capturedAppointment.serviceId).isEqualTo(testServiceId)
        assertThat(capturedAppointment.serviceName).isEqualTo(testServiceName)
        assertThat(capturedAppointment.servicePriceInCents).isEqualTo(testServicePriceInCents)
        assertThat(capturedAppointment.durationMinutes).isEqualTo(testServiceDuration)
        assertThat(capturedAppointment.appointmentDateTime.seconds).isEqualTo(expectedStartInstant.epochSecond)
        assertThat(capturedAppointment.customerName).isEqualTo(testCustomerName.trim())
        assertThat(capturedAppointment.customerPhone).isEqualTo(testCustomerPhone.trim())
        assertThat(capturedAppointment.customerEmail).isEqualTo(testCustomerEmail?.trim())
        assertThat(capturedAppointment.createdAt).isNull()

        val capturedSlot = slotSlot.captured
        val expectedEndInstant = expectedStartInstant.plusSeconds(testServiceDuration * 60L)

        assertThat(capturedSlot.ownerId).isEqualTo(testOwnerId)
        assertThat(capturedSlot.appointmentId).isEqualTo(testAppointmentId)
        assertThat(capturedSlot.endTime.seconds).isEqualTo(expectedEndInstant.epochSecond)
    }

    @Test
    fun `invoke() with blank userId returns failure without calling repositories`() = runTest {
        // Act
        val actualResult = createAppointmentUseCase(
            ownerId = testOwnerId,
            servicePriceInCents = testServicePriceInCents,
            userId = "",
            serviceId = testServiceId,
            serviceName = testServiceName,
            serviceDuration = testServiceDuration,
            date = testDate,
            time = testTime,
            customerName = testCustomerName,
            customerPhone = testCustomerPhone,
            customerEmail = testCustomerEmail
        )
        // Assert
        assertThat(actualResult).isInstanceOf(Result.Error::class.java)
        val exception = (actualResult as Result.Error).exception

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception.message).isEqualTo("User ID cannot be blank.")

        coVerify(exactly = 0) { mockAppointmentRepository.createAppointmentWithId(any(), any()) }
        coVerify(exactly = 0) { mockSlotRepository.addSlot(any()) }
    }

    @Test
    fun `invoke() with blank serviceId returns failure without calling repositories`() = runTest {
        // Act
        val actualResult = createAppointmentUseCase(
            testUserId,
            testServicePriceInCents,
            testOwnerId,
            " ",
            testServiceName,
            testServiceDuration,
            testDate,
            testTime,
            testCustomerName,
            testCustomerPhone,
            testCustomerEmail
        )
        // Assert
        assertThat(actualResult).isInstanceOf(Result.Error::class.java)
        val exception = (actualResult as Result.Error).exception

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception.message).isEqualTo("Service ID cannot be blank.")

        coVerify(exactly = 0) { mockAppointmentRepository.createAppointmentWithId(any(), any()) }
    }


    @Test
    fun `invoke() with blank name return failure without calling repositories`() = runTest {
        // Act
        val actualResult = createAppointmentUseCase(
            testUserId,
            testServicePriceInCents,
            testOwnerId,
            testServiceId,
            testServiceName,
            testServiceDuration,
            testDate,
            testTime,
            " ",
            testCustomerPhone,
            testCustomerEmail
        )
        // Assert
        assertThat(actualResult).isInstanceOf(Result.Error::class.java)
        val exception = (actualResult as Result.Error).exception

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception.message).isEqualTo("Customer name and phone cannot be empty.")
    }

    @Test
    fun `invoke() with blank phone return failure without calling repositories`() = runTest {
        // Act
        val actualResult = createAppointmentUseCase(
            testUserId,
            testServicePriceInCents,
            testOwnerId,
            testServiceId,
            testServiceName,
            testServiceDuration,
            testDate,
            testTime,
            testCustomerName,
            " ",
            testCustomerEmail
        )
        // Assert
        assertThat(actualResult).isInstanceOf(Result.Error::class.java)
        val exception = (actualResult as Result.Error).exception

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception.message).isEqualTo("Customer name and phone cannot be empty.")
    }

    @Test
    fun `invoke() with blank email should trim and set email to null`() = runTest {
        // Arrange
        val appointmentSlot = slot<Appointment>()
        // Başarılı davranışlar setUp'ta ayarlandı.

        // Act
        val result = createAppointmentUseCase(
            testOwnerId,
            testServicePriceInCents,
            testUserId,
            testServiceId,
            testServiceName,
            testServiceDuration,
            testDate,
            testTime,
            testCustomerName,
            testCustomerPhone,
            testCustomerEmailEmpty
        )

        // Assert
        assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify(exactly = 1) { mockAppointmentRepository.createAppointmentWithId(any(), capture(appointmentSlot)) }
        assertThat(appointmentSlot.captured.customerEmail).isNull()
    }

    @Test
    fun `invoke() when appointment repository fails and does not call slot repository`() = runTest {
        // ARRANGE
        val repositoryException = Exception("Firestore error")
        coEvery { mockAppointmentRepository.createAppointmentWithId(any(), any()) } returns Result.Error(repositoryException)

        val actualResult = createAppointmentUseCase(
            testOwnerId,
            testServicePriceInCents,
            testUserId,
            testServiceId,
            testServiceName,
            testServiceDuration,
            testDate,
            testTime,
            testCustomerName,
            testCustomerPhone,
            testCustomerEmail
        )

        assertThat(actualResult).isInstanceOf(Result.Error::class.java)
        assertThat((actualResult as Result.Error).exception).isEqualTo(repositoryException)

        coVerify(exactly = 1) { mockAppointmentRepository.createAppointmentWithId(any(), any()) }
        coVerify(exactly = 0) { mockSlotRepository.addSlot(any()) }
    }

    @Test
    fun `invoke() when slot repository fails return failure`() = runTest {
        // ARRANGE
        val slotRepositoryException = Exception("Slot error")
        coEvery { mockSlotRepository.addSlot(any()) } returns Result.Error(slotRepositoryException)

        // ACT
        val actualResult = createAppointmentUseCase(
            testOwnerId,
            testServicePriceInCents,
            testUserId,
            testServiceId,
            testServiceName,
            testServiceDuration,
            testDate,
            testTime,
            testCustomerName,
            testCustomerPhone,
            testCustomerEmail
        )

        // ASSERT
        assertThat(actualResult).isInstanceOf(Result.Error::class.java)
        assertThat((actualResult as Result.Error).exception).isEqualTo(slotRepositoryException)

        coVerify(exactly = 1) { mockAppointmentRepository.createAppointmentWithId(any(), any()) }
        coVerify(exactly = 1) { mockSlotRepository.addSlot(any()) }
    }
}