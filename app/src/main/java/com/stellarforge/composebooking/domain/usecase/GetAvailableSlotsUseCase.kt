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

        // 1. Define Business Constraints
        val openingTime = BusinessConstants.OPENING_TIME // e.g. 09:00
        val closingTime = BusinessConstants.CLOSING_TIME // e.g. 18:00
        val interval = BusinessConstants.SLOT_INTERVAL_MINUTES.toLong() // e.g. 15 min

        // 2. Time Validation (Prevent past bookings)
        val now = LocalTime.now()
        val isToday = date == LocalDate.now()

        // Calculate buffer: e.g., Users can't book a slot starting in the next 30 mins
        val bufferMinutes = BusinessConstants.MIN_BOOKING_NOTICE_MINUTES.toLong()
        val earliestBookableTime = if (isToday) now.plusMinutes(bufferMinutes) else LocalTime.MIN

        // 3. Pre-process Busy Intervals (Convert Firestore Timestamp -> LocalTime)
        // We convert them once here to avoid repeated conversions inside the loop.
        val zoneId = ZoneId.systemDefault()
        val busyIntervals = bookedSlots.map { slot ->
            val start = slot.startTime.toDate().toInstant().atZone(zoneId).toLocalTime()
            val end = slot.endTime.toDate().toInstant().atZone(zoneId).toLocalTime()
            start to end // Pair(Start, End)
        }

        // 4. Algorithm: Iterate from Opening Time to Closing Time
        var currentSlotStart = openingTime

        // Ensure the service finishes before or exactly at closing time
        while (currentSlotStart.plusMinutes(serviceDurationMinutes.toLong()) <= closingTime) {

            // Rule A: Past Time Check
            // Skip this slot if it's in the past or within the buffer zone
            if (isToday && currentSlotStart.isBefore(earliestBookableTime)) {
                currentSlotStart = currentSlotStart.plusMinutes(interval)
                continue
            }

            // Rule B: OVERLAP CHECK (Collision Detection)
            // Calculate when the requested service would end
            val currentSlotEnd = currentSlotStart.plusMinutes(serviceDurationMinutes.toLong())

            var isOverlapping = false

            for ((bookedStart, bookedEnd) in busyIntervals) {
                // Overlap Formula:
                // (NewStart < OldEnd) AND (NewEnd > OldStart)
                //
                // Example Scenario:
                // Existing Booking: 15:00 - 15:45
                // Requested Slot:   15:15 - 15:30 (e.g., Manicure)
                // Logic: 15:15 < 15:45 (True) AND 15:30 > 15:00 (True) -> COLLISION!

                if (currentSlotStart.isBefore(bookedEnd) && currentSlotEnd.isAfter(bookedStart)) {
                    isOverlapping = true
                    break // Optimization: Stop checking if one collision is found
                }
            }

            // If no collision, this slot is available
            if (!isOverlapping) {
                availableTimeSlots.add(currentSlotStart)
            }

            // Move to the next interval (e.g., 09:00 -> 09:15)
            currentSlotStart = currentSlotStart.plusMinutes(interval)
        }

        return availableTimeSlots
    }
}