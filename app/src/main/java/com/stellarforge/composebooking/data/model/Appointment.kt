package com.stellarforge.composebooking.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a confirmed Booking/Appointment entity in the system.
 * Maps directly to the 'appointments' collection in Firestore.
 *
 * **Data Integrity Note:**
 * Fields like [serviceName] and [servicePriceInCents] are "snapshotted" at the time of booking.
 * This ensures that even if the business owner changes the service price later,
 * the historical record of this specific appointment remains accurate.
 */
data class Appointment (
    val id: String = "",

    @get:PropertyName("ownerId")
    val ownerId: String = "",

    @get:PropertyName("userId")
    val userId: String = "",

    @get:PropertyName("serviceId")
    val serviceId: String = "",

    // Snapshotted data (Historical accuracy)
    @get:PropertyName("serviceName")
    val serviceName: String = "",

    @get:PropertyName("servicePriceInCents")
    val servicePriceInCents: Long = 0L,

    @get:PropertyName("durationMinutes")
    val durationMinutes: Int = 0,

    @get:PropertyName("appointmentDateTime")
    val appointmentDateTime: Timestamp = Timestamp.now(),

    // Customer Contact Info (Snapshot)
    @get:PropertyName("customerName")
    val customerName: String = "",

    @get:PropertyName("customerPhone")
    val customerPhone: String = "",

    @get:PropertyName("customerEmail")
    val customerEmail: String? = null,

    @ServerTimestamp
    @get:PropertyName("createdAt")
    val createdAt: Date? = null
)