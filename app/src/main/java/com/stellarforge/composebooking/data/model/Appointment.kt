package com.stellarforge.composebooking.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Firestore'daki 'appointments' koleksiyonundaki belgeleri temsil edecek data class
data class Appointment (
    val id: String = "",

    @get:PropertyName("ownerId")
    val ownerId: String = "",

    @get:PropertyName("userId")
    val userId: String = "",

    @get:PropertyName("serviceId")
    val serviceId: String = "",

    @get:PropertyName("serviceName")
    val serviceName: String = "",

    // YENİ EKLENEN KRİTİK ALAN
    @get:PropertyName("servicePriceInCents")
    val servicePriceInCents: Long = 0L,

    @get:PropertyName("durationMinutes")
    val durationMinutes: Int = 0,

    @get:PropertyName("appointmentDateTime")
    val appointmentDateTime: Timestamp = Timestamp.now(),

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