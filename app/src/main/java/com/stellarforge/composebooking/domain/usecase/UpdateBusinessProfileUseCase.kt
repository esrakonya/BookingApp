package com.stellarforge.composebooking.domain.usecase

import com.google.firebase.Timestamp
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.data.repository.AuthRepository
import com.stellarforge.composebooking.data.repository.BusinessProfileRepository
import com.stellarforge.composebooking.utils.Result
import timber.log.Timber
import javax.inject.Inject

class UpdateBusinessProfileUseCase @Inject constructor(
    private val businessProfileRepository: BusinessProfileRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(profileToUpdate: BusinessProfile): Result<Unit> {
        Timber.d("UpdateBusinessProfileUseCse invoked with profile: $profileToUpdate")

        // Get current user
        val authUserResult = authRepository.getCurrentUser()

        return when (authUserResult) {
            is Result.Success -> {
                val authUser = authUserResult.data
                if (authUser != null && authUser.uid.isNotBlank()) {
                    Timber.i("UpdateBusinessProfileUseCase: Current authUser found: ${authUser.uid}. Proceeding with update.")

                    val currentTime = Timestamp.now()
                    val profileWithTimestamps = profileToUpdate.copy(
                        createdAt = profileToUpdate.createdAt ?: currentTime,
                        updatedAt = currentTime
                    )
                    Timber.d("UpdateBusinessProfileUseCase: Profile with timestamps: $profileWithTimestamps")
                    businessProfileRepository.updateBusinessProfile(authUser.uid, profileWithTimestamps)
                } else {
                    val errorMessage = "User not authenticated or UID is blank. Cannot update business profile."
                    Timber.w("UpdateBusinessProfileUseCase: $errorMessage")
                    Result.Error(Exception(errorMessage))
                }
            }
            is Result.Error -> {
                Timber.e(authUserResult.exception, "UpdateBusinessProfileUseCase: Error getting current user. Message: ${authUserResult.message}")
                Result.Error(authUserResult.exception, authUserResult.message ?: "Failed to get current user information for update.")
            }
            is Result.Loading -> {
                Timber.d("UpdateBusinessProfileUseCase: GetCurrentUser is Loading, Cannot proceed with update yet.")
                Result.Error(Exception("User information is still loading, please try again."), "User data not ready.")
            }
        }
    }
}