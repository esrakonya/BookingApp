package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.domain.repository.AuthRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

/**
 * UseCase responsible for terminating the current user session.
 *
 * **Architectural Importance:**
 * While currently a simple delegation to the repository, this UseCase is the designated
 * place for any **Session Cleanup Logic**.
 *
 * Examples of future logic to add here:
 * - Clearing local Room database cache (user-specific data).
 * - Unsubscribing from Firebase Cloud Messaging (FCM) topics.
 * - Sending a "Logout" analytics event.
 */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Executes the sign-out process.
     *
     * @return [Result.Success] if the session was cleared successfully, or [Result.Error] otherwise.
     */
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.signOut()
    }
}