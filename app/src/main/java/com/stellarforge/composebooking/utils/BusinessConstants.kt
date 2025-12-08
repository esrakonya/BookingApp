package com.stellarforge.composebooking.utils

/**
 * Configuration object for Business Logic rules.
 *
 * Use this file to define opening hours, slot intervals, and booking restrictions.
 * These constants are used by [GetAvailableSlotsUseCase] to calculate logic.
 */
object BusinessConstants {
    val OPENING_TIME: java.time.LocalTime = java.time.LocalTime.of(9, 0)
    val CLOSING_TIME: java.time.LocalTime = java.time.LocalTime.of(18, 0)

    const val SLOT_INTERVAL_MINUTES = 15

    /**
     * Minimum time (in minutes) required before a booking can be made.
     * Example: If set to 30, and it's currently 14:00, the earliest slot shown will be 14:30.
     * This prevents last-minute surprise bookings for the business owner.
     */
    const val MIN_BOOKING_NOTICE_MINUTES = 30
}