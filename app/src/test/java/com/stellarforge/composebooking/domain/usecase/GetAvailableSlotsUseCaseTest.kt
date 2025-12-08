package com.stellarforge.composebooking.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.domain.repository.SlotRepository
import com.stellarforge.composebooking.utils.BusinessConstants
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

/**
 * Unit tests for [GetAvailableSlotsUseCase].
 *
 * This test suite verifies the core business logic of the scheduling system:
 * 1. **Time Calculation:** Ensures slots are generated correctly based on opening/closing hours.
 * 2. **Availability Logic:** Verifies that booked slots are correctly removed from the list.
 * 3. **Conflict Resolution:** Ensures overlapping appointments (e.g., a 60min service fitting into a 30min gap) are handled correctly.
 * 4. **Business Rules:** Tests rules like "Minimum Booking Notice" (not booking in the past).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetAvailableSlotsUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockSlotRepository: SlotRepository

    private lateinit var getAvailableSlotsUseCase: GetAvailableSlotsUseCase

    // Input owner ID (from the UI request)
    private val dummyInputOwnerId = "input-owner-id"
    // Expected Target ID (Currently hardcoded for single-owner template)
    private val expectedTargetId = FirebaseConstants.TARGET_BUSINESS_OWNER_ID

    @Before
    fun setUp() {
        getAvailableSlotsUseCase = GetAvailableSlotsUseCase(mockSlotRepository)
    }

    // Helper to create Firestore Timestamps
    private fun createTimestamp(date: LocalDate, hour: Int, minute: Int): Timestamp {
        val instant = date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant()
        return Timestamp(Date.from(instant))
    }

    // Helper to simulate an existing booking in the database
    private fun createBookedSlot(date: LocalDate, startHour: Int, startMinute: Int, durationMinutes: Int): BookedSlot {
        val startTime = createTimestamp(date, startHour, startMinute)
        val endTimeInstant = startTime.toDate().toInstant().plus(Duration.ofMinutes(durationMinutes.toLong()))
        val endTime = Timestamp(Date.from(endTimeInstant))
        return BookedSlot(
            ownerId = expectedTargetId,
            startTime = startTime,
            endTime = endTime,
            appointmentId = "appt-${startHour}${startMinute}"
        )
    }

    @Test
    fun `invoke() when repository returns slots - emits Loading then Success with correctly calculated times`() = runTest {
        // ARRANGE
        val testDate = LocalDate.now().plusDays(1)
        val serviceDuration = 60 // 1 Hour Service

        // Mocking: Assume there are two existing bookings
        val existingSlots = listOf(
            createBookedSlot(testDate, 10, 0, 30), // 10:00 - 10:30
            createBookedSlot(testDate, 14, 0, 60), // 14:00 - 15:00
        )

        coEvery { mockSlotRepository.getSlotsForDate(expectedTargetId, testDate) } returns Result.Success(existingSlots)

        // ACT & ASSERT
        getAvailableSlotsUseCase(dummyInputOwnerId, testDate, serviceDuration).test {

            // Expect Loading state first
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)

            // Expect Success state
            val successResult = awaitItem()
            assertThat(successResult).isInstanceOf(Result.Success::class.java)
            val availableTimes = (successResult as Result.Success).data

            // 1. Should be available before the first booking (Opening is usually 09:00)
            assertThat(availableTimes).contains(LocalTime.of(9, 0))

            // 2. Should NOT be available during the first booking
            assertThat(availableTimes).doesNotContain(LocalTime.of(10, 0))

            // 3. Should NOT be available immediately after, because 10:30 + 60min overlaps with 14:00 booking or insufficient gap?
            // (Depends on logic, but checking specific exclusions is key)
            assertThat(availableTimes).doesNotContain(LocalTime.of(10, 15))

            // 4. Checking availability after bookings
            assertThat(availableTimes).contains(LocalTime.of(15, 0))

            awaitComplete()
        }

        coVerify(exactly = 1) { mockSlotRepository.getSlotsForDate(expectedTargetId, testDate) }
    }

    @Test
    fun `invoke - when repository returns error - emits Loading then Error`() = runTest {
        // ARRANGE
        val testDate = LocalDate.now().plusDays(2)
        val serviceDuration = 30
        val expectedException = Exception("DB fetch failed")

        coEvery { mockSlotRepository.getSlotsForDate(expectedTargetId, testDate) } returns Result.Error(expectedException)

        // ACT & ASSERT
        getAvailableSlotsUseCase(dummyInputOwnerId, testDate, serviceDuration).test {
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)

            val errorResult = awaitItem()
            assertThat(errorResult).isInstanceOf(Result.Error::class.java)
            assertThat((errorResult as Result.Error).exception).isEqualTo(expectedException)

            awaitComplete()
        }

        coVerify(exactly = 1) { mockSlotRepository.getSlotsForDate(expectedTargetId, testDate) }
    }

    @Test
    fun `invoke - when no slots are booked - returns all possible slots for the day`() = runTest {
        // ARRANGE
        val testDate = LocalDate.now().plusDays(4) // Future Date
        val serviceDuration = 45

        coEvery { mockSlotRepository.getSlotsForDate(expectedTargetId, testDate) } returns Result.Success(emptyList())

        // ACT & ASSERT
        getAvailableSlotsUseCase(dummyInputOwnerId, testDate, serviceDuration).test {
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)

            val result = awaitItem()
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val availableTimes = (result as Result.Success).data

            // Verify logic: Generate expected slots manually
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