package com.stellarforge.composebooking.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Data class representing the business profile information.
 * This information will be stored in Firestore under `/businesses/{ownerUserId}`.
 */
data class BusinessProfile(
    @get:PropertyName("business_name")
    @set:PropertyName("business_name")
    var businessName: String = "",

    @get:PropertyName("contact_email")
    @set:PropertyName("contact_email")
    var contactEmail: String? = null,

    @get:PropertyName("contact_phone")
    @set:PropertyName("contact_phone")
    var contactPhone: String? = null,

    @get:PropertyName("address")
    @set:PropertyName("address")
    var address: String? = null,

    @get:PropertyName("logo_url")
    @set:PropertyName("logo_url")
    var logoUrl: String? = null,

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    var createdAt: com.google.firebase.Timestamp? = null,
    @get:PropertyName("updated_at") @set:PropertyName("updated_at")
    var updatedAt: com.google.firebase.Timestamp? = null

) {
    constructor() : this(
        businessName = "",
        contactEmail = null,
        contactPhone = null,
        address = null,
        logoUrl = null,
        createdAt = null,
        updatedAt = null
    )
}
