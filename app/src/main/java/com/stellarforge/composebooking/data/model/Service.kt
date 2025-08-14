package com.stellarforge.composebooking.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

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
    val price: Long = 0L,

    @get:PropertyName("isActive")
    val isActive: Boolean = true,

    @ServerTimestamp
    @get:PropertyName("createdAt")
    val createdAt: Date? = null,

    @ServerTimestamp
    @get:PropertyName("updatedAt")
    val updatedAt: Date? = null

)