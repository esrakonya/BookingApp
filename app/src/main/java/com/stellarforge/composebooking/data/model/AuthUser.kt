package com.stellarforge.composebooking.data.model

data class AuthUser(
    val uid: String,
    val email: String? = null,
    val isAnonymous: Boolean = false,
    val role: String = "customer"
)