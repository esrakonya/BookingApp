package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.domain.repository.AuthRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

/**
 * UseCase responsible for registering a new user account via Email/Password.
 *
 * **Security & Role Management:**
 * This class enforces strict role assignment rules. It acts as a gatekeeper to ensure
 * that a user is explicitly defined as either a 'customer' or an 'owner' before
 * hitting the database. This prevents creating users with undefined or malicious roles.
 */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Executes the registration logic.
     *
     * @param email The desired email address.
     * @param password The desired password.
     * @param role The role to assign ("customer" or "owner").
     * @return [Result.Success] with the new [AuthUser] if successful, or [Result.Error] if validation fails.
     */
    suspend operator fun invoke(email: String, password: String, role: String): Result<AuthUser> {
        // 1. Basic Input Validation
        if (email.isBlank() || password.isBlank()) {
            return Result.Error(IllegalArgumentException("Email or password cannot be blank."))
        }

        // 2. Security Validation: Strict Allow-list for roles
        // Prevents injection of invalid roles like "admin", "superuser", etc.
        if (role != "customer" && role != "owner") {
            return Result.Error(IllegalArgumentException("Invalid user role specified: $role"))
        }

        return authRepository.signUpWithEmailPassword(email, password, role)
    }
}