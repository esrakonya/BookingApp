package com.stellarforge.composebooking.data.model

/**
 * Domain representation of an Authenticated User.
 *
 * This class abstracts the underlying `FirebaseUser` object, providing a clean
 * and UI-agnostic model for the rest of the application.
 *
 * @property role Critical field for Role-Based Access Control (RBAC).
 * Values should be either "customer" or "owner".
 */
data class AuthUser(
    val uid: String,
    val email: String? = null,
    val isAnonymous: Boolean = false,
    val role: String = "customer"
)