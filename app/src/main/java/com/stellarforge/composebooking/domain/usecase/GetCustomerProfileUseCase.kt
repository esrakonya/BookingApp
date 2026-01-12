package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.CustomerProfile
import com.stellarforge.composebooking.domain.repository.CustomerProfileRepository
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase responsible for retrieving and monitoring the Customer's personal profile data.
 *
 * **Reactive Architecture:**
 * Instead of a one-time fetch, this UseCase returns a [Flow]. This ensures that the
 * UI (e.g., "My Account" screen) stays perfectly synchronized with the database in real-time.
 * If the user updates their profile (e.g., changes name), the UI updates automatically without a refresh.
 */
class GetCustomerProfileUseCase @Inject constructor(
    private val customerProfileRepository: CustomerProfileRepository
) {
    /**
     * Retrieves the real-time profile stream for a specific user.
     *
     * @param userId The unique identifier of the customer (Auth UID).
     * @return A [Flow] emitting [Result] states (Loading, Success with Data, or Error).
     */
    operator fun invoke(userId: String): Flow<Result<CustomerProfile>> {
        return customerProfileRepository.getCustomerProfileStream(userId)
    }
}