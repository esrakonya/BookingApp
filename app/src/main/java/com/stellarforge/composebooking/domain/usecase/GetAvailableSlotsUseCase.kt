package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.domain.repository.SlotRepository
import com.stellarforge.composebooking.utils.BusinessConstants
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Core Business Logic for calculating available time slots.
 *
 * This UseCase performs the following complex operations:
 * 1. Fetches "Booked Slots" from the repository for a specific date.
 * 2. Generates potential time slots based on Opening/Closing hours and Interval (e.g., every 15 mins).
 * 3. Filters out slots based on:
 *    - **Overlap Logic:** Checks if the service duration clashes with an existing booking.
 *    - **Time Rules:** Ensures bookings cannot be made in the past.
 *    - **Buffer Time:** Enforces [BusinessConstants.MIN_BOOKING_NOTICE_MINUTES].
 *
 * @param slotRepository Repository to fetch existing bookings.
 */
class GetAvailableSlotsUseCase @Inject constructor(
    private val slotRepository: SlotRepository
) {
    // We use the 'operator fun invoke' to make the UseCase callable like a function
    operator fun invoke(ownerId: String, date: LocalDate, serviceDuration: Int): Flow<Result<List<LocalTime>>> = flow {
        Timber.d("GetAvailableSlotsUseCase: Invoked for date: $date, duration: $serviceDuration")
        emit(Result.Loading)

        // Hardcoded ID for Single-Tenant Architecture (White Label)
        val targetOwnerId = FirebaseConstants.TARGET_BUSINESS_OWNER_ID

        try {
            // 1. Fetch existing bookings from Firestore
            when (val slotsResult = slotRepository.getSlotsForDate(targetOwnerId, date)) {
                is Result.Success -> {
                    val bookedSlots = slotsResult.data
                    Timber.d("UseCase: Fetched ${bookedSlots.size} booked slots for date: $date")

                    // 2. Perform the heavy calculation
                    val availableSlots = calculateAvailableSlots(bookedSlots, serviceDuration, date)

                    // 3. Emit the calculated list
                    emit(Result.Success(availableSlots))
                }
                is Result.Error -> {
                    Timber.e(slotsResult.exception, "UseCase: Error fetching slots from repository.")
                    emit(slotsResult)
                }
                is Result.Loading -> {
                    emit(Result.Loading)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "UseCase: Error calculating available slots")
            val wrappedException = Exception("UseCase: Error calculating available slots for $date", e)
            emit(Result.Error(wrappedException))
        }
    }.catch { e ->
        Timber.e(e, "GetAvailableSlotsUseCase: Unhandled error in flow chain.")
        emit(Result.Error(Exception("UseCase: Unhandled error calculating available slots", e)))
    }.flowOn(Dispatchers.Default) // CPU-intensive work runs on Default Dispatcher


    /**
     * The Algorithm:
     * Iterates from Opening Time to Closing Time, checking each interval against
     * existing bookings and business rules.
     */
    private fun calculateAvailableSlots(
        bookedSlots: List<BookedSlot>,
        serviceDurationMinutes: Int,
        date: LocalDate
    ): List<LocalTime> {
        val availableTimeSlots = mutableListOf<LocalTime>()

        // Business Rules
        val openingTime = BusinessConstants.OPENING_TIME
        val closingTime = BusinessConstants.CLOSING_TIME
        val interval = BusinessConstants.SLOT_INTERVAL_MINUTES.toLong()
        val minNoticeMinutes = BusinessConstants.MIN_BOOKING_NOTICE_MINUTES

        // Time Constraints
        val now = LocalTime.now()
        val isToday = date == LocalDate.now()

        // If today, enforce the "Buffer Time" rule (e.g., cannot book 5 mins from now)
        val earliestBookingTime = if (isToday) now.plusMinutes(minNoticeMinutes.toLong()) else LocalTime.MIN

        // Pre-process: Convert Firestore Timestamps to LocalTime for easier comparison
        val bookedIntervals = bookedSlots.map {
            val start = it.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
            val end = it.endTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
            start to end
        }.sortedBy { it.first }

        var potentialStartTime = openingTime

        // Loop: Generate slots
        while (potentialStartTime.plusMinutes(serviceDurationMinutes.toLong()) <= closingTime) {

            // Rule 1: Check against "Past Time" or "Buffer Time"
            if (isToday && potentialStartTime.isBefore(earliestBookingTime)) {
                potentialStartTime = potentialStartTime.plusMinutes(interval)
                continue
            }

            val potentialEndTime = potentialStartTime.plusMinutes(serviceDurationMinutes.toLong())
            var isSlotAvailable = true

            // Rule 2: Check for Overlaps with existing bookings
            for ((bookedStart, bookedEnd) in bookedIntervals) {
                // Overlap Formula: (StartA < EndB) and (EndA > StartB)
                if (potentialStartTime < bookedEnd && potentialEndTime > bookedStart) {
                    isSlotAvailable = false
                    break // Optimization: Stop checking other bookings if one conflict is found
                }
            }

            if (isSlotAvailable) {
                availableTimeSlots.add(potentialStartTime)
            }

            // Move to next interval (e.g., 09:00 -> 09:15)
            potentialStartTime = potentialStartTime.plusMinutes(interval)
        }

        return availableTimeSlots
    }
}