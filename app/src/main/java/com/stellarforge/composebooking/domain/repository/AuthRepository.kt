package com.stellarforge.composebooking.domain.repository

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.withContext

/**
 * Defines the contract for Authentication and User Session management within the Domain layer.
 *
 * This interface follows the Dependency Inversion Principle (DIP):
 * UseCases depend on this abstraction, not the concrete implementation in the Data layer.
 * It acts as a single source of truth for the user's identity.
 */
interface AuthRepository {

    /**
     * Retrieves the currently authenticated user session.
     *
     * @return A [Result] containing the [AuthUser] if logged in, or `null` if the user is anonymous/signed out.
     */
    suspend fun getCurrentUser(): Result<AuthUser?>

    /**
     * Authenticates a user using their email and password credentials.
     *
     * @param email The user's registered email address.
     * @param password The user's password.
     * @return A [Result] containing the authenticated [AuthUser] data upon success.
     */
    suspend fun signInWithEmailPassword(email: String, password: String): Result<AuthUser>

    /**
     * Registers a new user account with a specific role.
     *
     * **Role Management:**
     * The [role] parameter is crucial for the application's security model.
     * It dictates whether the new user is a 'customer' (can book services) or an 'owner' (can manage the shop).
     *
     * @param email The email address for the new account.
     * @param password The password for the new account.
     * @param role The user role string (e.g., "customer", "owner").
     */
    suspend fun signUpWithEmailPassword(email: String, password: String, role: String): Result<AuthUser>

    /**
     * Terminates the current user session.
     * Clears any local authentication tokens.
     */
    suspend fun signOut(): Result<Unit>
}