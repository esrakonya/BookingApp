package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.data.repository.BusinessProfileRepository
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject

@ExperimentalCoroutinesApi
class GetBusinessProfileUseCase @Inject constructor(
    private val businessProfileRepository: BusinessProfileRepository
) {
    operator fun invoke(ownerUserId: String): Flow<Result<BusinessProfile?>> {
        Timber.d("GetBusinessProfileUseCase invoked.")

        if (ownerUserId.isBlank()) {
            Timber.w("GetBusinessProfileUseCase: ownerUserId is blank. Emitting error.")

            return flow { emit(Result.Error(IllegalArgumentException("Owner user ID cannot be blank."))) }
        }

        return businessProfileRepository.getBusinessProfile(ownerUserId)
            .onStart {
                Timber.d("GetBusinessProfileUseCase: getBusinessProfile Flow for $ownerUserId started, emitting Loading.")
                emit(Result.Loading)
            }
            .catch { e ->
                Timber.e(e, "GetBusinessProfileUseCase: Error in getBusinessProfile Flow for $ownerUserId")
                emit(Result.Error(Exception("Failed to get business profile: ${e.localizedMessage}", e)))
            }
    }
}