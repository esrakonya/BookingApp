package com.stellarforge.composebooking.data.model

import com.google.firebase.firestore.PropertyName

data class Service (
    var id: String = "",

    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("description") @set:PropertyName("description")
    var description: String = "",

    @get:PropertyName("durationMinutes") @set:PropertyName("durationMinutes")
    var durationMinutes: Int = 30,

    @get:PropertyName("price") @set:PropertyName("price")
    var price: Double = 0.0,

    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true

)