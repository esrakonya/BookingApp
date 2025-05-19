package com.stellarforge.composebooking.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.Timestamp

/**
 * Firestore'daki 'bookedSlots' koleksiyonundaki belgeleri temsil eder.
 * Sadece müsait saat hesaplaması için gerekli olan zaman bilgilerini içerir.
 * Hassas müşteri bilgisi içermez.
 */
data class BookedSlot(
    // Firestore'dan okurken belge ID'sini atamak isteyebiliriz, ama hesaplama için şart değil.
    // var id: String = "",

    // Hangi servise ait olduğu bilgisi, farklı süreler için önemli olabilir mi?
    // Şimdilik gerekli görünmüyor ama eklenebilir:
    // @get:PropertyName("serviceId") @set:PropertyName("serviceId")
    // var serviceId: String = "",

    @get:PropertyName("startTime") @set:PropertyName("startTime")
    var startTime: Timestamp = Timestamp.now(), // Randevunun başlangıç zamanı

    @get:PropertyName("endTime") @set:PropertyName("endTime")
    var endTime: Timestamp = Timestamp.now(), // Randevunun bitiş zamanı

    // Appointment ID'sine referans (opsiyonel, senkronizasyon için faydalı olabilir)
    @get:PropertyName("appointmentId") @set:PropertyName("appointmentId")
    var appointmentId: String = ""
)