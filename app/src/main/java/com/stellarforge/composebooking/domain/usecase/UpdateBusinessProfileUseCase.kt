package com.stellarforge.composebooking.domain.usecase

import com.google.firebase.Timestamp
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.domain.repository.AuthRepository
import com.stellarforge.composebooking.domain.repository.BusinessProfileRepository
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase responsible for updating the Business Owner's public profile (Storefront details).
 *
 * **Architectural Enforcer (White-Label Security):**
 * This class implements a critical safety mechanism for the Single-Tenant architecture.
 * Regardless of which admin user is currently logged in, this UseCase forces all profile updates
 * to be written to the specific [FirebaseConstants.TARGET_BUSINESS_OWNER_ID].
 *
 * This guarantees that the Customer App (which listens to that specific ID) always stays
 * synchronized with the data being edited in the Owner App, preventing data fragmentation.
 */
class UpdateBusinessProfileUseCase @Inject constructor(
    private val businessProfileRepository: BusinessProfileRepository,
    private val authRepository: AuthRepository
) {
    /**
     * Executes the update logic.
     *
     * @param profileToUpdate The new profile data entered by the owner.
     * @return [Result.Success] if updated, or [Result.Error] if authentication fails or network error occurs.
     */
    suspend operator fun invoke(profileToUpdate: BusinessProfile): Result<Unit> {
        Timber.d("UpdateBusinessProfileUseCase: Invoked.")

        // 1. Verify Authentication
        val authUserResult = authRepository.getCurrentUser()

        return when (authUserResult) {
            is Result.Success -> {
                val authUser = authUserResult.data

                if (authUser != null && authUser.uid.isNotBlank()) {
                    // 2. Identify the Target Document
                    // We use the constant ID to ensure data consistency across the platform.
                    val targetOwnerId = FirebaseConstants.TARGET_BUSINESS_OWNER_ID

                    // Security Audit Log: Check if the logged-in user matches the configured owner.
                    if (authUser.uid != targetOwnerId) {
                        Timber.e("SECURITY WARNING: The logged-in user (${authUser.uid}) does not match the Target Owner ID ($targetOwnerId). Proceeding with update to Target ID to maintain app function.")
                    }

                    Timber.i("UpdateBusinessProfileUseCase: Writing profile data to Target Owner ID: $targetOwnerId")

                    // 3. Prepare Data with Timestamps
                    val currentTime = Timestamp.now()
                    val profileWithTimestamps = profileToUpdate.copy(
                        createdAt = profileToUpdate.createdAt ?: currentTime, // Keep original creation time if exists
                        updatedAt = currentTime // Always update modification time
                    )

                    // 4. Execute Update
                    businessProfileRepository.updateBusinessProfile(targetOwnerId, profileWithTimestamps)

                } else {
                    val msg = "User not authenticated. Cannot update profile."
                    Timber.w("UpdateBusinessProfileUseCase: $msg")
                    Result.Error(Exception(msg))
                }
            }
            is Result.Error -> {
                Timber.e(authUserResult.exception, "UpdateBusinessProfileUseCase: Failed to retrieve user session.")
                Result.Error(authUserResult.exception, "Authentication failed.")
            }
            is Result.Loading -> {
                Result.Error(Exception("Operation in progress"), "Loading...")
            }
        }
    }
}