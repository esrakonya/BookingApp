package com.stellarforge.composebooking.data.remote

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.utils.Result

/**
 * Defines the contract for remote authentication operations.
 *
 * This interface abstracts the underlying authentication provider (e.g., Firebase Auth)
 * from the repository layer. It handles the lifecycle of a user session, including
 * login, registration with specific roles (Customer vs. Owner), and session retrieval.
 */
interface AuthRemoteDataSource {
    suspend fun getCurrentUser(): Result<AuthUser?>

    suspend fun signInWithEmailPassword(email: String, password: String): Result<AuthUser>

    suspend fun signUpWithEmailPassword(email: String, password: String, role: String): Result<AuthUser>

    suspend fun signOut(): Result<Unit>
}