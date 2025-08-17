package com.stellarforge.composebooking.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore'daki 'bookedSlots' koleksiyonundaki belgeleri temsil eder.
 * Sadece müsait saat hesaplaması için gerekli olan zaman bilgilerini içerir.
 * Hassas müşteri bilgisi içermez.
 */
data class BookedSlot(
    val id: String = "",

    @get:PropertyName("ownerId")
    val ownerId: String = "",

    @get:PropertyName("appointmentId")
    val appointmentId: String = "",

    @get:PropertyName("startTime")
    val startTime: Timestamp = Timestamp.now(), // Randevunun başlangıç zamanı

    @get:PropertyName("endTime") @set:PropertyName("endTime")
    var endTime: Timestamp = Timestamp.now(), // Randevunun bitiş zamanı

    @ServerTimestamp
    @get:PropertyName("createdAt")
    val createdAt: Date? = null

)