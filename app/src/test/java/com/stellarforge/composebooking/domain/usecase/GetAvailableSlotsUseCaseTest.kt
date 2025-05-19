package com.stellarforge.composebooking.domain.usecase

import app.cash.turbine.test
import com.google.firebase.Timestamp
import com.stellarforge.composebooking.R // Gerekirse R importu
import com.stellarforge.composebooking.data.model.BookedSlot // Appointment yerine BookedSlot
import com.stellarforge.composebooking.data.repository.SlotRepository // AppointmentRepository yerine SlotRepository
import com.stellarforge.composebooking.utils.BusinessConstants
import com.stellarforge.composebooking.utils.MainDispatcherRule // Dispatcher Rule kullanalım
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.*

@OptIn(ExperimentalCoroutinesApi::class)
class GetAvailableSlotsUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule() // StandardTestDispatcher kullanır

    // Mock Repository (SlotRepository türünde)
    private lateinit var mockSlotRepository: SlotRepository
    // Test Edilecek Sınıf
    private lateinit var getAvailableSlotsUseCase: GetAvailableSlotsUseCase

    @Before
    fun setUp() {
        // mockSlotRepository'yi oluştur
        mockSlotRepository = mockk(relaxed = true)
        // UseCase'i mockSlotRepository ile oluştur
        getAvailableSlotsUseCase = GetAvailableSlotsUseCase(mockSlotRepository)
    }

    // Firestore Timestamp oluşturma yardımcısı (öncekiyle aynı)
    private fun createTimestamp(date: LocalDate, hour: Int, minute: Int): Timestamp {
        val localDateTime = LocalDateTime.of(date, LocalTime.of(hour, minute))
        val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()
        return Timestamp(instant.epochSecond, instant.nano)
    }

    // BookedSlot oluşturma yardımcısı
    private fun createBookedSlot(date: LocalDate, startHour: Int, startMinute: Int, durationMinutes: Int, appointmentId: String = "appt-${startHour}${startMinute}"): BookedSlot {
        val startTime = createTimestamp(date, startHour, startMinute)
        val endTime = createTimestamp(date, startHour, startMinute).let { start ->
            val startInstant = Instant.ofEpochSecond(start.seconds, start.nanoseconds.toLong())
            val endInstant = startInstant.plus(Duration.ofMinutes(durationMinutes.toLong()))
            Timestamp(endInstant.epochSecond, endInstant.nano)
        }
        return BookedSlot(startTime = startTime, endTime = endTime, appointmentId = appointmentId)
    }


    @Test
    fun `invoke() when repository returns slots, should return correct available times`() = runTest {
        // Arrange
        val testDate = LocalDate.of(2025, 7, 15)
        val serviceDuration = 60 // Servis süresi

        // Mevcut dolu slotlar (BookedSlot nesneleri)
        val existingSlots = listOf(
            createBookedSlot(testDate, 10, 0, 30), // 10:00 - 10:30
            createBookedSlot(testDate, 14, 0, 60), // 14:00 - 15:00
            createBookedSlot(testDate, 17, 30, 30)  // 17:30 - 18:00
        )
        val successResult: Result<List<BookedSlot>> = Result.success(existingSlots)

        // mockSlotRepository.getSlotsForDate metodu çağrıldığında successResult dönsün
        coEvery { mockSlotRepository.getSlotsForDate(testDate) } returns successResult

        // Act & Assert
        getAvailableSlotsUseCase(testDate, serviceDuration).test {
            val result = awaitItem() // Flow'dan gelen Result<List<LocalTime>>
            assertTrue("Result should be success", result.isSuccess)

            val availableTimes = result.getOrNull()
            assertNotNull("Available times should not be null", availableTimes)

            // Beklenen müsait saatler (öncekiyle aynı mantıkla hesaplanmalı)
            val expectedTimes = listOf(
                LocalTime.of(9, 0),   // 09:00 - 10:00 (müsait)
                // 10:00 - 10:30 dolu
                LocalTime.of(10, 30), // 10:30 - 11:30 (müsait)
                LocalTime.of(10, 45), // 10:45 - 11:45 (müsait)
                LocalTime.of(11, 0),  // 11:00 - 12:00 (müsait)
                LocalTime.of(11, 15), // 11:15 - 12:15 (müsait)
                LocalTime.of(11, 30), // 11:30 - 12:30 (müsait)
                LocalTime.of(11, 45), // 11:45 - 12:45 (müsait)
                LocalTime.of(12, 0),  // 12:00 - 13:00 (müsait)
                LocalTime.of(12, 15), // 12:15 - 13:15 (müsait)
                LocalTime.of(12, 30), // 12:30 - 13:30 (müsait)
                LocalTime.of(12, 45), // 12:45 - 13:45 (müsait)
                LocalTime.of(13, 0),  // 13:00 - 14:00 (müsait)
                // 14:00 - 15:00 dolu
                LocalTime.of(15, 0),  // 15:00 - 16:00 (müsait)
                LocalTime.of(15, 15), // 15:15 - 16:15 (müsait)
                LocalTime.of(15, 30), // 15:30 - 16:30 (müsait)
                LocalTime.of(15, 45), // 15:45 - 16:45 (müsait)
                LocalTime.of(16, 0),  // 16:00 - 17:00 (müsait)
                LocalTime.of(16, 15), // 16:15 - 17:15 (müsait)
                LocalTime.of(16, 30)  // 16:30 - 17:30 (müsait)
                // 17:30 - 18:00 dolu
            )

            assertEquals(expectedTimes, availableTimes)

            awaitComplete() // Flow'un tamamlanmasını bekle
        }
        coVerify(exactly = 1) { mockSlotRepository.getSlotsForDate(testDate) }
    }

    @Test
    fun `invoke() when repository returns failure, should emit failure`() = runTest {
        // Arrange
        val testDate = LocalDate.of(2025, 7, 16)
        val serviceDuration = 30
        val expectedException = Exception("Failed to fetch slots from mock")
        val failureResult: Result<List<BookedSlot>> = Result.failure(expectedException)

        coEvery { mockSlotRepository.getSlotsForDate(testDate) } returns failureResult

        // Act & Assert
        getAvailableSlotsUseCase(testDate, serviceDuration).test {
            val result = awaitItem()
            assertTrue("Result should be failure", result.isFailure)
            // Hatanın kendisini veya mesajını kontrol edebiliriz
            assertEquals(expectedException, result.exceptionOrNull())
            awaitComplete()
        }
        coVerify(exactly = 1) { mockSlotRepository.getSlotsForDate(testDate) }
    }

    @Test
    fun `invoke() when calculation logic itself throws (edge case), should emit failure`() = runTest {
        // Arrange
        val testDate = LocalDate.of(2025, 7, 17)
        val serviceDuration = 30
        // Repository'nin exception fırlatmasını simüle edelim
        val repoException = RuntimeException("Repo connection failed")
        coEvery { mockSlotRepository.getSlotsForDate(testDate) } throws repoException

        // Act & Assert
        getAvailableSlotsUseCase(testDate, serviceDuration).test {
            val result = awaitItem()
            assertTrue("Result should be failure when repo throws", result.isFailure)
            // UseCase'deki catch bloğu hatayı sarmalamalı
            assertTrue("Exception message should indicate calculation error", result.exceptionOrNull()?.message?.contains("UseCase: Error calculating available slots") == true)
            assertEquals("Cause should be the original repo exception", repoException, result.exceptionOrNull()?.cause)
            awaitComplete()
        }
        coVerify(exactly = 1) { mockSlotRepository.getSlotsForDate(testDate) }
    }

    @Test
    fun `invoke() when no slots booked, should return all slots within working hours`() = runTest {
        // Arrange
        val testDate = LocalDate.of(2025, 7, 18)
        val serviceDuration = 45
        val timeInterval = BusinessConstants.SLOT_INTERVAL_MINUTES
        val openingTime = BusinessConstants.OPENING_TIME
        val closingTime = BusinessConstants.CLOSING_TIME

        // Boş liste döndür
        coEvery { mockSlotRepository.getSlotsForDate(testDate) } returns Result.success(emptyList<BookedSlot>())

        // Act & Assert
        getAvailableSlotsUseCase(testDate, serviceDuration).test {
            val result = awaitItem()
            assertTrue("Result should be success", result.isSuccess)
            val availableTimes = result.getOrNull() ?: emptyList()

            // Beklenen tüm slotları hesapla
            val expectedTimes = mutableListOf<LocalTime>()
            var currentTime = openingTime
            while (currentTime.plusMinutes(serviceDuration.toLong()) <= closingTime) {
                expectedTimes.add(currentTime)
                currentTime = currentTime.plusMinutes(timeInterval.toLong())
            }

            assertEquals(expectedTimes, availableTimes)
            awaitComplete()
        }
        coVerify(exactly = 1) { mockSlotRepository.getSlotsForDate(testDate) }
    }
}