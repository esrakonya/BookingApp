package com.stellarforge.composebooking.data.repository

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.remote.AuthRemoteDataSource
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.di.IoDispatcher
import com.stellarforge.composebooking.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Concrete implementation of [AuthRepository] that mediates between the Domain layer
 * and the Data source.
 *
 * **Key Responsibilities:**
 * - **Abstraction:** Hides the details of the underlying auth provider (Firebase) from the rest of the app.
 * - **Concurrency Management:** Wraps all suspend functions in [withContext] using the [IoDispatcher].
 *   This ensures that network operations never block the Main (UI) Thread, making the app "Main-Safe".
 */
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    /**
     * Checks the current session status.
     * Returns [AuthUser] if logged in, or null/error otherwise.
     */
    override suspend fun getCurrentUser(): Result<AuthUser?> {
        return withContext(ioDispatcher) {
            authRemoteDataSource.getCurrentUser()
        }
    }

    override suspend fun signInWithEmailPassword(
        email: String,
        password: String
    ): Result<AuthUser> {
        return withContext(ioDispatcher) {
            authRemoteDataSource.signInWithEmailPassword(email, password)
        }
    }

    /**
     * Registers a new user.
     * IMPORTANT: Passes the [role] ('customer' or 'owner') to ensure the user is created
     * with the correct permissions in the database.
     */
    override suspend fun signUpWithEmailPassword(
        email: String,
        password: String,
        role: String
    ): Result<AuthUser> {
        return withContext(ioDispatcher) {
            authRemoteDataSource.signUpWithEmailPassword(email, password, role)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return withContext(ioDispatcher) {
            authRemoteDataSource.signOut()
        }
    }
}