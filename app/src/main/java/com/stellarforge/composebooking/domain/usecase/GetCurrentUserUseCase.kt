package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.domain.repository.AuthRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

/**
 * UseCase responsible for retrieving the currently authenticated user session.
 *
 * **Architectural Role:**
 * Although this currently acts as a simple pass-through to the [AuthRepository],
 * keeping this layer is crucial for Clean Architecture. It allows for future expansion
 * (e.g., refreshing tokens, checking user bans, or fetching additional metadata)
 * without modifying the UI code.
 */
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Executes the retrieval logic.
     *
     * @return A [Result] containing the [AuthUser] object if logged in,
     *         or [Result.Success] with `null` if the user is not authenticated (Guest).
     */
    suspend operator fun invoke(): Result<AuthUser?> {
        return authRepository.getCurrentUser()
    }
}