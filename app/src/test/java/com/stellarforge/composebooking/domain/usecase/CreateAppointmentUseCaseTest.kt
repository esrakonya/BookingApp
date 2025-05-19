package com.stellarforge.composebooking.domain.usecase

import com.google.firebase.firestore.DocumentReference
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.BookedSlot // Yeni import
import com.stellarforge.composebooking.data.repository.AppointmentRepository
import com.stellarforge.composebooking.data.repository.SlotRepository // Yeni import
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class CreateAppointmentUseCaseTest {

    // Mock Bağımlılıklar
    private lateinit var mockAppointmentRepository: AppointmentRepository
    private lateinit var mockSlotRepository: SlotRepository
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
        // Mock'ları oluştur (relaxed = true, varsayılan başarılı dönüşler için)
        mockAppointmentRepository = mockk(relaxed = true)
        mockSlotRepository = mockk(relaxed = true)
        mockDocumentReference = mockk()

        // Yeni ID alma ve ID ile oluşturma için temel davranışları ayarla
        every { mockAppointmentRepository.getNewAppointmentReference() } returns mockDocumentReference
        every { mockDocumentReference.id } returns testAppointmentId
        // relaxed=true olduğu için success döndürmelerini bekliyoruz, ama açıkça da belirtilebilir:
        coEvery { mockAppointmentRepository.createAppointmentWithId(any(), any()) } returns Result.success(Unit)
        coEvery { mockSlotRepository.addSlot(any()) } returns Result.success(Unit)


        // UseCase'i mock repository'ler ile oluştur
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
        assertTrue("Result should be successful", actualResult.isSuccess)

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
        assertTrue(actualResult.isFailure)
        assertTrue(actualResult.exceptionOrNull() is IllegalArgumentException)
        assertEquals("User ID cannot be blank.", actualResult.exceptionOrNull()?.message)
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
        assertTrue(actualResult.isFailure)
        assertTrue(actualResult.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Service ID cannot be blank.", actualResult.exceptionOrNull()?.message)
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
        assertTrue(actualResult.isFailure)
        assertTrue(actualResult.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Customer name and phone cannot be empty.", actualResult.exceptionOrNull()?.message)
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
        assertTrue(actualResult.isFailure)
        assertTrue(actualResult.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Customer name and phone cannot be empty.", actualResult.exceptionOrNull()?.message)
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
        assertTrue(result.isSuccess) // İşlem başarılı olmalı
        coVerify(exactly = 1) {
            mockAppointmentRepository.createAppointmentWithId(any(), capture(appointmentSlot))
        }
        coVerify(exactly = 1) { mockSlotRepository.addSlot(any()) } // Slot da eklenmeli

        val capturedAppointment = appointmentSlot.captured
        assertNull("Email should be null when input is blank", capturedAppointment.customerEmail)
    }

    @Test
    fun `invoke() when appointment repository fails during createWithId should return failure and not call slot repository`() = runTest {
        // Arrange
        val repositoryException = Exception("Firestore appointment write failed")
        // ID alma başarılı olsun
        every { mockAppointmentRepository.getNewAppointmentReference() } returns mockDocumentReference
        every { mockDocumentReference.id } returns testAppointmentId
        // Ama ID ile yazma başarısız olsun
        coEvery { mockAppointmentRepository.createAppointmentWithId(mockDocumentReference, any()) } returns Result.failure(repositoryException)
        // Slot repository hiç çağrılmamalı
        coEvery { mockSlotRepository.addSlot(any()) } returns Result.success(Unit) // Bu çağrılmayacak

        // Act
        val actualResult = createAppointmentUseCase(
            userId = testUserId, serviceId = testServiceId, serviceName = testServiceName,
            serviceDuration = testServiceDuration, date = testDate, time = testTime,
            customerName = testCustomerName, customerPhone = testCustomerPhone, customerEmail = testCustomerEmail
        )

        // Assert
        assertTrue("Result should be failure", actualResult.isFailure) // Bu satır muhtemelen zaten vardı ve doğruydu
        val actualException = actualResult.exceptionOrNull()
        assertNotNull("Exception should not be null when appointment repo fails", actualException)
        assertTrue("Exception type should be Exception", actualException is Exception)
        assertEquals("Exception message should match", repositoryException.message, actualException?.message) // Mesajı karşılaştır

        coVerify(exactly = 1) { mockAppointmentRepository.getNewAppointmentReference() }
        coVerify(exactly = 1) { mockAppointmentRepository.createAppointmentWithId(mockDocumentReference, any()) }
        coVerify(exactly = 0) { mockSlotRepository.addSlot(any()) }
    }

    @Test
    fun `invoke() when slot repository fails should return failure`() = runTest {
        // Arrange
        val slotRepositoryException = Exception("Firestore slot write failed")
        // ID alma ve Appointment yazma başarılı olsun (setUp'ta ayarlı)
        // Slot yazma başarısız olsun
        coEvery { mockSlotRepository.addSlot(any()) } returns Result.failure(slotRepositoryException)

        // Act
        val actualResult = createAppointmentUseCase(
            userId = testUserId, serviceId = testServiceId, serviceName = testServiceName,
            serviceDuration = testServiceDuration, date = testDate, time = testTime,
            customerName = testCustomerName, customerPhone = testCustomerPhone, customerEmail = testCustomerEmail
        )

        // Assert
        assertTrue("Result should be failure when slot repo fails", actualResult.isFailure) // Bu satır doğruydu
        val actualException = actualResult.exceptionOrNull()
        assertNotNull("Exception should not be null when slot repo fails", actualException)
        assertTrue("Exception type should be Exception", actualException is Exception)
        assertEquals("Exception message should match", slotRepositoryException.message, actualException?.message) // Mesajı karşılaştır

        coVerify(exactly = 1) { mockAppointmentRepository.getNewAppointmentReference() }
        coVerify(exactly = 1) { mockAppointmentRepository.createAppointmentWithId(any(), any()) }
        coVerify(exactly = 1) { mockSlotRepository.addSlot(any()) }
    }
}