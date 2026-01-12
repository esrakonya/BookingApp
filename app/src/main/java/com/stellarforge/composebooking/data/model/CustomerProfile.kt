package com.stellarforge.composebooking.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Represents the extended user profile stored in Firestore 'customer' collection.
 * This contains editable fields like Name and Phone.
 */
data class CustomerProfile(
    val id: String = "",
    val email: String = "",
    val role: String = "customer",

    @get:PropertyName("name")
    val name: String? = null,

    @get:PropertyName("phone")
    val phone: String? = null
)