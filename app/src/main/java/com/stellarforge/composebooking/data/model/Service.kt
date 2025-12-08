package com.stellarforge.composebooking.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data model representing a Service offered by the business (e.g., Haircut, Manicure).
 *
 * This class maps directly to documents in the 'services' Firestore collection.
 * Key Details:
 * - **priceInCents:** Monetary values are stored as [Long] (e.g., $10.50 -> 1050) to avoid floating-point precision errors.
 * - **durationMinutes:** Used by the booking engine to calculate time slot availability.
 */
data class Service (
    val id: String = "",

    @get:PropertyName("ownerId")
    val ownerId: String = "",

    @get:PropertyName("name")
    val name: String = "",

    @get:PropertyName("description")
    val description: String = "",

    @get:PropertyName("durationMinutes")
    val durationMinutes: Int = 30,

    @get:PropertyName("priceInCents")
    val priceInCents: Long = 0L,

    @get:PropertyName("isActive")
    val isActive: Boolean = true,

    @ServerTimestamp
    @get:PropertyName("createdAt")
    val createdAt: Date? = null,

    @ServerTimestamp
    @get:PropertyName("updatedAt")
    val updatedAt: Date? = null

)