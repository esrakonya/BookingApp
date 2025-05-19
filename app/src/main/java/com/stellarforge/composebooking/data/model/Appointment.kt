package com.stellarforge.composebooking.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.util.Date

// Firestore'daki 'appointments' koleksiyonundaki belgeleri temsil edecek data class
data class Appointment (
    var id: String = "",

    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",

    @get:PropertyName("serviceId") @set:PropertyName("serviceId")
    var serviceId: String = "",

    @get:PropertyName("serviceName") @set:PropertyName("serviceName")
    var serviceName: String = "",

    @get:PropertyName("appointmentDateTime") @set:PropertyName("appointmentDateTime")
    var appointmentDateTime: Timestamp = Timestamp.now(),

    @get:PropertyName("durationMinutes") @set:PropertyName("durationMinutes")
    var durationMinutes: Int = 0,

    @get:PropertyName("customerName") @set:PropertyName("customerName")
    var customerName: String = "",

    @get:PropertyName("customerPhone") @set:PropertyName("customerPhone")
    var customerPhone: String = "",

    @get:PropertyName("customerEmail") @set:PropertyName("customerEmail")
    var customerEmail: String? = null,

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Timestamp = Timestamp.now()
)