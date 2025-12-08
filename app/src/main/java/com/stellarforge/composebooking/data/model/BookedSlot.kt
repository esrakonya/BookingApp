package com.stellarforge.composebooking.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents documents in the 'bookedSlots' collection in Firestore.
 * Contains only the time information required for calculating available slots.
 * Does not contain sensitive customer data.
 */
data class BookedSlot(
    val id: String = "",

    @get:PropertyName("ownerId")
    val ownerId: String = "",

    @get:PropertyName("appointmentId")
    val appointmentId: String = "",

    @get:PropertyName("startTime")
    val startTime: Timestamp = Timestamp.now(),

    @get:PropertyName("endTime") @set:PropertyName("endTime")
    var endTime: Timestamp = Timestamp.now(),

    @ServerTimestamp
    @get:PropertyName("createdAt")
    val createdAt: Date? = null

)