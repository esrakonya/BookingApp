package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.domain.repository.BusinessProfileRepository
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject

/**
 * UseCase responsible for retrieving the public-facing Business Profile (Storefront).
 *
 * **Real-time Updates:**
 * It returns a [Flow], meaning any changes made by the Owner (e.g., changing the phone number or name)
 * are immediately reflected on the Customer's screen without requiring a manual refresh.
 */
class GetBusinessProfileUseCase @Inject constructor(
    private val businessProfileRepository: BusinessProfileRepository
) {
    /**
     * @param ownerUserId The unique ID of the business owner (Target ID).
     * @return A Flow emitting Loading, Success (with Profile), or Error states.
     */
    operator fun invoke(ownerUserId: String): Flow<Result<BusinessProfile?>> {
        Timber.d("GetBusinessProfileUseCase invoked.")

        // Fast fail validation
        if (ownerUserId.isBlank()) {
            Timber.w("GetBusinessProfileUseCase: ownerUserId is blank.")
            return flow { emit(Result.Error(IllegalArgumentException("Owner user ID cannot be blank."))) }
        }

        return businessProfileRepository.getBusinessProfile(ownerUserId)
            .onStart {
                emit(Result.Loading)
            }
            .catch { e ->
                Timber.e(e, "GetBusinessProfileUseCase: Error in stream for $ownerUserId")
                emit(Result.Error(Exception("Failed to get business profile: ${e.localizedMessage}", e)))
            }
    }
}