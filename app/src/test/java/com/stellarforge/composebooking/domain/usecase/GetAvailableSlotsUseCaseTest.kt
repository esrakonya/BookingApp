package com.stellarforge.composebooking.domain.usecase

import app.cash.turbine.test
import com.google.firebase.Timestamp
import com.stellarforge.composebooking.data.model.BookedSlot // Appointment yerine BookedSlot
import com.stellarforge.composebooking.data.repository.SlotRepository // AppointmentRepository yerine SlotRepository
import com.stellarforge.composebooking.utils.BusinessConstants
import com.stellarforge.composebooking.utils.MainDispatcherRule // Dispatcher Rule kullanalım
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.*

@OptIn(ExperimentalCoroutinesApi::class)
class GetAvailableSlotsUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule() // StandardTestDispatcher kullanır

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockSlotRepository: SlotRepository
    // Test Edilecek Sınıf
    private lateinit var getAvailableSlotsUseCase: GetAvailableSlotsUseCase

    @Before
    fun setUp() {
        getAvailableSlotsUseCase = GetAvailableSlotsUseCase(mockSlotRepository)
    }

    // --- Yardımcı Fonksiyonlar ---
    private fun createTimestamp(date: LocalDate, hour: Int, minute: Int): Timestamp {
        val instant = date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant()
        return Timestamp(instant.epochSecond, instant.nano)
    }

    private fun createBookedSlot(date: LocalDate, startHour: Int, startMinute: Int, durationMinutes: Int): BookedSlot {
        val startTime = createTimestamp(date, startHour, startMinute)
        val endTimeInstant = Instant.ofEpochSecond(startTime.seconds, startTime.nanoseconds.toLong())
            .plus(Duration.ofMinutes(durationMinutes.toLong()))
        val endTime = Timestamp(endTimeInstant.epochSecond, endTimeInstant.nano)
        return BookedSlot(startTime = startTime, endTime = endTime, appointmentId = "appt-${startHour}${startMinute}")
    }


    @Test
    fun `invoke() when repository returns slots, should emit Loading then Success with correct times`() = runTest {
        // ARRANGE
        val testDate = LocalDate.now().plusDays(1)
        val serviceDuration = 60
        val existingSlots = listOf(
            createBookedSlot(testDate, 10, 0, 30),
            createBookedSlot(testDate, 14, 0, 60),
            createBookedSlot(testDate, 17, 30, 30)
        )
        coEvery { mockSlotRepository.getSlotsForDate(testDate) } returns Result.Success(existingSlots)

        // ACT & ASSERT
        getAvailableSlotsUseCase(testDate, serviceDuration).test {
            // DÜZELTİLDİ: JUnit Assert'leri kullanılıyor
            val loadingResult = awaitItem()
            assertTrue("First result should be Loading", loadingResult is Result.Loading)

            val successResult = awaitItem()
            assertTrue("Second result should be Success", successResult is Result.Success)
            val availableTimes = (successResult as Result.Success).data

            val expectedTimes = listOf(
                LocalTime.of(9, 0), LocalTime.of(10, 30), LocalTime.of(10, 45), LocalTime.of(11, 0),
                LocalTime.of(11, 15), LocalTime.of(11, 30), LocalTime.of(11, 45), LocalTime.of(12, 0),
                LocalTime.of(12, 15), LocalTime.of(12, 30), LocalTime.of(12, 45), LocalTime.of(13, 0),
                LocalTime.of(15, 0), LocalTime.of(15, 15), LocalTime.of(15, 30), LocalTime.of(15, 45),
                LocalTime.of(16, 0), LocalTime.of(16, 15), LocalTime.of(16, 30)
            )
            assertEquals(expectedTimes, availableTimes)

            awaitComplete()
        }
        coVerify(exactly = 1) { mockSlotRepository.getSlotsForDate(testDate) }
    }

    @Test
    fun `invoke() when repository returns error, should emit Loading then Error`() = runTest {
        // ARRANGE
        val testDate = LocalDate.now().plusDays(2)
        val serviceDuration = 30
        val expectedException = Exception("Failed to fetch slots from mock")
        coEvery { mockSlotRepository.getSlotsForDate(testDate) } returns Result.Error(expectedException)

        // ACT & ASSERT
        getAvailableSlotsUseCase(testDate, serviceDuration).test {
            // DÜZELTİLDİ: JUnit Assert'leri kullanılıyor
            assertTrue("First result should be Loading", awaitItem() is Result.Loading)

            val result = awaitItem()
            assertTrue("Second result should be Error", result is Result.Error)
            assertEquals(expectedException, (result as Result.Error).exception)

            awaitComplete()
        }
        coVerify(exactly = 1) { mockSlotRepository.getSlotsForDate(testDate) }
    }

    @Test
    fun `invoke() when repository throws, use case should catch and emit Loading then Error`() = runTest {
        // ARRANGE
        val testDate = LocalDate.now().plusDays(3)
        val serviceDuration = 30
        val repoException = RuntimeException("Repo connection failed")
        coEvery { mockSlotRepository.getSlotsForDate(testDate) } throws repoException

        // ACT & ASSERT
        getAvailableSlotsUseCase(testDate, serviceDuration).test {
            // DÜZELTİLDİ: JUnit Assert'leri kullanılıyor
            assertTrue("First result should be Loading", awaitItem() is Result.Loading)

            val result = awaitItem()
            assertTrue("Second result should be Error", result is Result.Error)

            val wrappedException = (result as Result.Error).exception
            assertTrue("Exception message should indicate calculation error", wrappedException.message!!.contains("UseCase: Error calculating available slots"))
            assertEquals("Cause should be the original repo exception", repoException, wrappedException.cause)

            awaitComplete()
        }
        coVerify(exactly = 1) { mockSlotRepository.getSlotsForDate(testDate) }
    }

    @Test
    fun `invoke() when no slots booked, should return all possible slots`() = runTest {
        // ARRANGE
        val testDate = LocalDate.now().plusDays(4)
        val serviceDuration = 45
        coEvery { mockSlotRepository.getSlotsForDate(testDate) } returns Result.Success(emptyList())

        // ACT & ASSERT
        getAvailableSlotsUseCase(testDate, serviceDuration).test {
            // DÜZELTİLDİ: JUnit Assert'leri kullanılıyor
            assertTrue("First result should be Loading", awaitItem() is Result.Loading)

            val result = awaitItem()
            assertTrue("Second result should be Success", result is Result.Success)
            val availableTimes = (result as Result.Success).data

            val expectedTimes = mutableListOf<LocalTime>()
            var currentTime = BusinessConstants.OPENING_TIME
            while (currentTime.plusMinutes(serviceDuration.toLong()) <= BusinessConstants.CLOSING_TIME) {
                expectedTimes.add(currentTime)
                currentTime = currentTime.plusMinutes(BusinessConstants.SLOT_INTERVAL_MINUTES.toLong())
            }

            assertEquals(expectedTimes, availableTimes)

            awaitComplete()
        }
        coVerify(exactly = 1) { mockSlotRepository.getSlotsForDate(testDate) }
    }
}