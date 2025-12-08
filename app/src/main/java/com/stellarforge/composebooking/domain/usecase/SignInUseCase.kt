package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.domain.repository.AuthRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

/**
 * UseCase responsible for authenticating an existing user via Email/Password.
 *
 * **Single Responsibility Principle (SRP):**
 * This class focuses solely on the "Sign In" transaction. It acts as a protective boundary
 * between the UI (ViewModel) and the Data Layer (Repository).
 */
class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Executes the sign-in logic.
     *
     * @param email The user's registered email.
     * @param password The user's password.
     * @return [Result.Success] with [AuthUser] if credentials are valid, or [Result.Error] otherwise.
     */
    suspend operator fun invoke(email: String, password: String): Result<AuthUser> {
        // Domain-Level Validation:
        // Even though the UI (ViewModel) performs input validation, the Domain layer
        // should never blindly trust external input. This "Defense in Depth" approach ensures
        // stability regardless of who calls this UseCase.
        if (email.isBlank() || password.isBlank()) {
            return Result.Error(IllegalArgumentException("Email and password cannot be blank."))
        }

        return authRepository.signInWithEmailPassword(email, password)
    }
}