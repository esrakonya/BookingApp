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
 * Verifies the core scheduling logic:
 * 1. **Calculation:** Generates correct intervals (e.g., 09:00, 09:15...).
 * 2. **Overlap Logic:** Ensures new bookings do not clash with existing ones.
 * 3. **Closing Time:** Ensures bookings do not exceed business hours.
 * 4. **Target Security:** Verifies that the app always queries the specific Target Owner ID.
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

    // We expect the UseCase to IGNORE this input and use the Constant instead.
    private val dummyInputOwnerId = "malicious-user-id"

    // The correct ID that must be used
    private val expectedTargetId = FirebaseConstants.TARGET_BUSINESS_OWNER_ID

    @Before
    fun setUp() {
        getAvailableSlotsUseCase = GetAvailableSlotsUseCase(mockSlotRepository)
    }

    // --- HELPERS ---
    private fun createBookedSlot(date: LocalDate, startHour: Int, startMinute: Int, durationMinutes: Int): BookedSlot {
        val startInstant = date.atTime(startHour, startMinute).atZone(ZoneId.systemDefault()).toInstant()
        val endInstant = startInstant.plus(Duration.ofMinutes(durationMinutes.toLong()))

        return BookedSlot(
            ownerId = expectedTargetId,
            startTime = Timestamp(Date.from(startInstant)),
            endTime = Timestamp(Date.from(endInstant)),
            appointmentId = "test-appt"
        )
    }

    @Test
    fun `invoke - handles COMPLEX OVERLAPS correctly`() = runTest {
        // ARRANGE
        val testDate = LocalDate.now().plusDays(1) // Tomorrow
        val serviceDuration = 30 // User wants 30 mins

        // SCENARIO:
        // Existing Appointment: 10:00 - 10:45 (45 mins long)
        // This effectively blocks the window from 10:00 to 10:45.
        val existingSlots = listOf(
            createBookedSlot(testDate, 10, 0, 45)
        )

        coEvery { mockSlotRepository.getSlotsForDate(expectedTargetId, testDate) } returns Result.Success(existingSlots)

        // ACT & ASSERT
        getAvailableSlotsUseCase(dummyInputOwnerId, testDate, serviceDuration).test {

            // Skip Loading
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)

            val result = awaitItem() as Result.Success
            val slots = result.data

            // 1. Before Conflict (09:30 - 10:00) -> OK
            assertThat(slots).contains(LocalTime.of(9, 30))

            // 2. Partial Overlap (09:45 - 10:15) -> BLOCKED
            // Starts before, but ends inside the existing booking.
            assertThat(slots).doesNotContain(LocalTime.of(9, 45))

            // 3. Exact Start (10:00 - 10:30) -> BLOCKED
            assertThat(slots).doesNotContain(LocalTime.of(10, 0))

            // 4. Inside (10:15 - 10:45) -> BLOCKED
            assertThat(slots).doesNotContain(LocalTime.of(10, 15))

            // 5. Partial Overlap End (10:30 - 11:00) -> BLOCKED
            // Starts inside, ends after.
            assertThat(slots).doesNotContain(LocalTime.of(10, 30))

            // 6. Exact End (10:45 - 11:15) -> OK
            // Previous booking finishes at 10:45. Chair is free.
            assertThat(slots).contains(LocalTime.of(10, 45))

            awaitComplete()
        }
    }

    @Test
    fun `invoke - filters out slots exceeding CLOSING TIME`() = runTest {
        // ARRANGE
        val testDate = LocalDate.now().plusDays(1)
        val serviceDuration = 60 // 1 Hour Service
        val closingTime = BusinessConstants.CLOSING_TIME // e.g., 18:00

        // No existing bookings
        coEvery { mockSlotRepository.getSlotsForDate(any(), any()) } returns Result.Success(emptyList())

        // ACT
        getAvailableSlotsUseCase(dummyInputOwnerId, testDate, serviceDuration).test {
            val slots = (awaitItem() as? Result.Success)?.data ?: (awaitItem() as Result.Success).data

            // 1. Last possible slot: 17:00 (Ends 18:00) -> OK
            val lastSlot = closingTime.minusMinutes(serviceDuration.toLong())
            assertThat(slots).contains(lastSlot)

            // 2. Too late: 17:15 (Ends 18:15) -> BLOCKED
            val tooLateSlot = lastSlot.plusMinutes(15)
            assertThat(slots).doesNotContain(tooLateSlot)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invoke - forces usage of TARGET_BUSINESS_OWNER_ID regardless of input`() = runTest {
        // ARRANGE
        val testDate = LocalDate.now().plusDays(1)
        coEvery { mockSlotRepository.getSlotsForDate(any(), any()) } returns Result.Success(emptyList())

        // ACT
        // We pass "dummyInputOwnerId", but the UseCase should ignore it.
        getAvailableSlotsUseCase(dummyInputOwnerId, testDate, 30).test {
            awaitItem() // Loading
            awaitItem() // Success
            cancelAndIgnoreRemainingEvents()
        }

        // ASSERT
        // Verify repository was called with the CONSTANT ID, not the dummy input.
        coVerify(exactly = 1) {
            mockSlotRepository.getSlotsForDate(
                ownerId = expectedTargetId, // Must match constant
                date = testDate
            )
        }
    }
}