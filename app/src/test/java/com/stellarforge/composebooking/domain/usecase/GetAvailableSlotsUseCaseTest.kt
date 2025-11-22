package com.stellarforge.composebooking.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import com.stellarforge.composebooking.data.model.BookedSlot // Appointment yerine BookedSlot
import com.stellarforge.composebooking.domain.repository.SlotRepository // AppointmentRepository yerine SlotRepository
import com.stellarforge.composebooking.utils.BusinessConstants
import com.stellarforge.composebooking.utils.MainDispatcherRule // Dispatcher Rule kullanalım
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.*
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class GetAvailableSlotsUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule() // StandardTestDispatcher kullanır

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK private lateinit var mockSlotRepository: SlotRepository
    private lateinit var getAvailableSlotsUseCase: GetAvailableSlotsUseCase

    private val testOwnerId = "test-business-owner-id"

    @Before
    fun setUp() {
        getAvailableSlotsUseCase = GetAvailableSlotsUseCase(mockSlotRepository)
    }

    // --- Yardımcı Fonksiyonlar ---
    private fun createTimestamp(date: LocalDate, hour: Int, minute: Int): Timestamp {
        val instant = date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant()
        return Timestamp(Date.from(instant))
    }

    private fun createBookedSlot(date: LocalDate, startHour: Int, startMinute: Int, durationMinutes: Int): BookedSlot {
        val startTime = createTimestamp(date, startHour, startMinute)
        val endTimeInstant = startTime.toDate().toInstant().plus(Duration.ofMinutes(durationMinutes.toLong()))
        val endTime = Timestamp(Date.from(endTimeInstant))
        return BookedSlot(
            ownerId = testOwnerId,
            startTime = startTime,
            endTime = endTime,
            appointmentId = "appt-${startHour}${startMinute}"
        )
    }


    @Test
    fun `invoke() when repository returns slots - emits Loading then Success with correctly calculated times`() = runTest {
        // ARRANGE
        val testDate = LocalDate.now().plusDays(1)
        val serviceDuration = 60
        val existingSlots = listOf(
            createBookedSlot(testDate, 10, 0, 30), //10:00 - 10:30 dolu
            createBookedSlot(testDate, 14, 0, 60), //14:00 - 15:00 dolu
        )
        coEvery { mockSlotRepository.getSlotsForDate(testOwnerId, testDate) } returns Result.Success(existingSlots)

        // ACT & ASSERT
        getAvailableSlotsUseCase(testOwnerId, testDate, serviceDuration).test {
            // DÜZELTİLDİ: JUnit Assert'leri kullanılıyor
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)

            val successResult = awaitItem()
            assertThat(successResult).isInstanceOf(Result.Success::class.java)
            val availableTimes = (successResult as Result.Success).data

            assertThat(availableTimes).doesNotContain(LocalTime.of(10, 0))
            assertThat(availableTimes).contains(LocalTime.of(10, 30))
            assertThat(availableTimes).doesNotContain(LocalTime.of(14, 0))
            assertThat(availableTimes).contains(LocalTime.of(15, 0))

            awaitComplete()
        }
        coVerify(exactly = 1) { mockSlotRepository.getSlotsForDate(testOwnerId, testDate) }
    }

    @Test
    fun `invoke - when repository returns error - emits Loading then Error`() = runTest {
        // ARRANGE
        val testDate = LocalDate.now().plusDays(2)
        val serviceDuration = 30
        val expectedException = Exception("DB fetch failed")
        coEvery { mockSlotRepository.getSlotsForDate(testOwnerId, testDate) } returns Result.Error(expectedException)

        // ACT & ASSERT
        getAvailableSlotsUseCase(testOwnerId, testDate, serviceDuration).test {
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)
            val errorResult = awaitItem()
            assertThat(errorResult).isInstanceOf(Result.Error::class.java)
            assertThat((errorResult as Result.Error).exception).isEqualTo(expectedException)

            awaitComplete()
        }
    }

    @Test
    fun `invoke - when no slots are booked - returns all possible slots for the day`() = runTest {
        // ARRANGE
        val testDate = LocalDate.now().plusDays(4)
        val serviceDuration = 45
        coEvery { mockSlotRepository.getSlotsForDate(testOwnerId, testDate) } returns Result.Success(emptyList())

        // ACT & ASSERT
        getAvailableSlotsUseCase(testOwnerId, testDate, serviceDuration).test {
            // DÜZELTİLDİ: JUnit Assert'leri kullanılıyor
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)

            val result = awaitItem()
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val availableTimes = (result as Result.Success).data

            val expectedTimes = mutableListOf<LocalTime>()
            var currentTime = BusinessConstants.OPENING_TIME
            while (currentTime.plusMinutes(serviceDuration.toLong()) <= BusinessConstants.CLOSING_TIME) {
                expectedTimes.add(currentTime)
                currentTime = currentTime.plusMinutes(BusinessConstants.SLOT_INTERVAL_MINUTES.toLong())
            }

            assertThat(availableTimes).isEqualTo(expectedTimes)

            awaitComplete()
        }
    }
}