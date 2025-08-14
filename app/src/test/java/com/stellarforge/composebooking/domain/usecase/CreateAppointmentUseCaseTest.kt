package com.stellarforge.composebooking.domain.usecase

import com.google.firebase.firestore.DocumentReference
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.BookedSlot // Yeni import
import com.stellarforge.composebooking.data.repository.AppointmentRepository
import com.stellarforge.composebooking.data.repository.SlotRepository // Yeni import
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
    private val testCustomerEmailEmpty: String? = "  " // Boşluklu boş email

    @Before
    fun setUp() {
        every { mockAppointmentRepository.getNewAppointmentReference() } returns mockDocumentReference
        every { mockDocumentReference.id } returns testAppointmentId
        // Başarılı durumları açıkça belirtelim
        coEvery { mockAppointmentRepository.createAppointmentWithId(any(), any()) } returns Result.Success(Unit)
        coEvery { mockSlotRepository.addSlot(any()) } returns Result.Success(Unit)

        createAppointmentUseCase = CreateAppointmentUseCase(
            mockAppointmentRepository,
            mockSlotRepository
        )
    }

    @Test
    fun `invoke() with valid data should call both repositories, create correct objects, and return success`() = runTest {
        // Arrange
        val appointmentSlot = slot<Appointment>()
        val slotSlot = slot<BookedSlot>()

        // Başarılı davranışlar setUp'ta ayarlandı.

        // Act
        val actualResult = createAppointmentUseCase(
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

        // Assert - Sonuç Başarılı mı?
        assertTrue("Result should be successful", actualResult is Result.Success)

        // Verify - Doğru metotlar çağrıldı mı?
        coVerify(exactly = 1) { mockAppointmentRepository.getNewAppointmentReference() }
        coVerify(exactly = 1) { mockAppointmentRepository.createAppointmentWithId(mockDocumentReference, capture(appointmentSlot)) }
        coVerify(exactly = 1) { mockSlotRepository.addSlot(capture(slotSlot)) }

        // Assert - Yakalanan Appointment Doğru mu?
        val capturedAppointment = appointmentSlot.captured
        assertEquals(testAppointmentId, capturedAppointment.id)
        assertEquals(testUserId, capturedAppointment.userId)
        assertEquals(testServiceId, capturedAppointment.serviceId)
        assertEquals(testServiceName, capturedAppointment.serviceName)
        assertEquals(testServiceDuration, capturedAppointment.durationMinutes)
        assertEquals(testCustomerName.trim(), capturedAppointment.customerName)
        assertEquals(testCustomerPhone.trim(), capturedAppointment.customerPhone)
        assertEquals(testCustomerEmail?.trim(), capturedAppointment.customerEmail)
        assertNotNull("Appointment DateTime should not be null", capturedAppointment.appointmentDateTime)
        assertNotNull("Created At should not be null", capturedAppointment.createdAt)
        // İsteğe bağlı: appointmentDateTime'ın doğruluğunu kontrol et
        val expectedStartInstant = testDate.atTime(testTime).atZone(ZoneId.systemDefault()).toInstant()
        assertEquals(expectedStartInstant.epochSecond, capturedAppointment.appointmentDateTime.seconds)

        // Assert - Yakalanan BookedSlot Doğru mu?
        val capturedSlot = slotSlot.captured
        assertEquals(testAppointmentId, capturedSlot.appointmentId)
        assertNotNull(capturedSlot.startTime)
        assertNotNull(capturedSlot.endTime)
        assertEquals(expectedStartInstant.epochSecond, capturedSlot.startTime.seconds)
        val expectedEndInstant = testDate.atTime(testTime).plusMinutes(testServiceDuration.toLong()).atZone(ZoneId.systemDefault()).toInstant()
        assertEquals(expectedEndInstant.epochSecond, capturedSlot.endTime.seconds)
    }

    @Test
    fun `invoke() with blank userId should return failure without calling repositories`() = runTest {
        // Act
        val actualResult = createAppointmentUseCase(
            userId = "", // Boş userId
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
        assertTrue("Result should be Error", actualResult is Result.Error)
        val errorResult = actualResult as Result.Error
        assertTrue(errorResult.exception is IllegalArgumentException)
        assertEquals("User ID cannot be blank.", errorResult.exception.message)
        // Hiçbir repository metodu çağrılmamalı (ID alma dahil)
        coVerify(exactly = 0) { mockAppointmentRepository.getNewAppointmentReference() }
        coVerify(exactly = 0) { mockAppointmentRepository.createAppointmentWithId(any(), any()) }
        coVerify(exactly = 0) { mockSlotRepository.addSlot(any()) }
    }

    @Test
    fun `invoke() with blank serviceId should return failure without calling repositories`() = runTest {
        // Act
        val actualResult = createAppointmentUseCase(
            userId = testUserId,
            serviceId = " ", // Boş serviceId
            serviceName = testServiceName,
            serviceDuration = testServiceDuration,
            date = testDate,
            time = testTime,
            customerName = testCustomerName,
            customerPhone = testCustomerPhone,
            customerEmail = testCustomerEmail
        )
        // Assert
        assertTrue("Result should be Error", actualResult is Result.Error)
        val errorResult = actualResult as Result.Error
        assertTrue(errorResult.exception is IllegalArgumentException)
        assertEquals("Service ID cannot be blank.", errorResult.exception.message)
        coVerify(exactly = 0) { mockAppointmentRepository.getNewAppointmentReference() }
        coVerify(exactly = 0) { mockAppointmentRepository.createAppointmentWithId(any(), any()) }
        coVerify(exactly = 0) { mockSlotRepository.addSlot(any()) }
    }


    @Test
    fun `invoke() with blank name should return failure without calling repositories`() = runTest {
        // Act
        val actualResult = createAppointmentUseCase(
            userId = testUserId,
            serviceId = testServiceId,
            serviceName = testServiceName,
            serviceDuration = testServiceDuration,
            date = testDate,
            time = testTime,
            customerName = "  ", // Boş isim
            customerPhone = testCustomerPhone,
            customerEmail = testCustomerEmail
        )
        // Assert
        assertTrue("Result should be Error", actualResult is Result.Error)
        val errorResult = actualResult as Result.Error
        assertTrue(errorResult.exception is IllegalArgumentException)
        assertEquals("Customer name and phone cannot be empty.", actualResult.exception.message)
        coVerify(exactly = 0) { mockAppointmentRepository.getNewAppointmentReference() }
        coVerify(exactly = 0) { mockAppointmentRepository.createAppointmentWithId(any(), any()) }
        coVerify(exactly = 0) { mockSlotRepository.addSlot(any()) }
    }

    @Test
    fun `invoke() with blank phone should return failure without calling repositories`() = runTest {
        // Act
        val actualResult = createAppointmentUseCase(
            userId = testUserId,
            serviceId = testServiceId,
            serviceName = testServiceName,
            serviceDuration = testServiceDuration,
            date = testDate,
            time = testTime,
            customerName = testCustomerName,
            customerPhone = "\t", // Boş telefon
            customerEmail = testCustomerEmail
        )
        // Assert
        assertTrue(actualResult is Result.Error)
        val errorResult = actualResult as Result.Error
        assertTrue(errorResult.exception is IllegalArgumentException)
        assertEquals("Customer name and phone cannot be empty.", errorResult.exception.message)
        coVerify(exactly = 0) { mockAppointmentRepository.getNewAppointmentReference() }
        coVerify(exactly = 0) { mockAppointmentRepository.createAppointmentWithId(any(), any()) }
        coVerify(exactly = 0) { mockSlotRepository.addSlot(any()) }
    }

    @Test
    fun `invoke() with blank email should trim and set email to null in created appointment`() = runTest {
        // Arrange
        val appointmentSlot = slot<Appointment>()
        // Başarılı davranışlar setUp'ta ayarlandı.

        // Act
        val result = createAppointmentUseCase(
            userId = testUserId,
            serviceId = testServiceId,
            serviceName = testServiceName,
            serviceDuration = testServiceDuration,
            date = testDate,
            time = testTime,
            customerName = testCustomerName,
            customerPhone = testCustomerPhone,
            customerEmail = testCustomerEmailEmpty // Boşluklu boş email
        )

        // Assert
        assertTrue(result is Result.Success) // İşlem başarılı olmalı
        coVerify(exactly = 1) {
            mockAppointmentRepository.createAppointmentWithId(any(), capture(appointmentSlot))
        }
        coVerify(exactly = 1) { mockSlotRepository.addSlot(any()) } // Slot da eklenmeli

        val capturedAppointment = appointmentSlot.captured
        assertNull("Email should be null when input is blank", capturedAppointment.customerEmail)
    }

    @Test
    fun `invoke() when appointment repository fails during createWithId should return failure and not call slot repository`() = runTest {
        // ARRANGE
        val repositoryException = Exception("Firestore appointment write failed")
        coEvery { mockAppointmentRepository.createAppointmentWithId(mockDocumentReference, any()) } returns Result.Error(repositoryException)

        // ACT
        val actualResult = createAppointmentUseCase(
            userId = testUserId, serviceId = testServiceId, serviceName = testServiceName,
            serviceDuration = testServiceDuration, date = testDate, time = testTime,
            customerName = testCustomerName, customerPhone = testCustomerPhone, customerEmail = testCustomerEmail
        )

        // ASSERT
        // DÜZELTİLDİ:
        assertTrue("Result should be an instance of Result.Error", actualResult is Result.Error)
        val errorResult = actualResult as Result.Error

        // Exception türünü kontrol etmek yerine, doğrudan nesneyi karşılaştıralım.
        // Bu hem türü hem de içeriği (mesajı) doğrular.
        assertEquals(repositoryException, errorResult.exception)

        // Verify
        coVerify(exactly = 1) { mockAppointmentRepository.getNewAppointmentReference() }
        coVerify(exactly = 1) { mockAppointmentRepository.createAppointmentWithId(mockDocumentReference, any()) }
        coVerify(exactly = 0) { mockSlotRepository.addSlot(any()) }
    }

    @Test
    fun `invoke() when slot repository fails should return failure`() = runTest {
        // ARRANGE
        val slotRepositoryException = Exception("Firestore slot write failed")
        coEvery { mockSlotRepository.addSlot(any()) } returns Result.Error(slotRepositoryException)

        // ACT
        val actualResult = createAppointmentUseCase(
            userId = testUserId, serviceId = testServiceId, serviceName = testServiceName,
            serviceDuration = testServiceDuration, date = testDate, time = testTime,
            customerName = testCustomerName, customerPhone = testCustomerPhone, customerEmail = testCustomerEmail
        )

        // ASSERT
        // DÜZELTİLDİ:
        assertTrue("Result should be an instance of Result.Error", actualResult is Result.Error)
        val errorResult = actualResult as Result.Error

        // Doğrudan exception nesnelerini karşılaştır.
        assertEquals(slotRepositoryException, errorResult.exception)

        // Verify
        coVerify(exactly = 1) { mockAppointmentRepository.getNewAppointmentReference() }
        coVerify(exactly = 1) { mockAppointmentRepository.createAppointmentWithId(any(), any()) }
        coVerify(exactly = 1) { mockSlotRepository.addSlot(any()) }
    }
}