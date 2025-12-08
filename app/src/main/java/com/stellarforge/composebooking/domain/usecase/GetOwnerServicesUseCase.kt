package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase responsible for retrieving the complete catalog of services for a specific business owner.
 *
 * **Purpose:**
 * Unlike the customer-facing stream, this UseCase fetches **ALL** services (both Active and Inactive).
 * It is designed for the "Manage Services" (Admin) dashboard, allowing the owner to see, edit,
 * or delete any service item, regardless of its visibility status.
 */
class GetOwnerServicesUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    /**
     * Executes the retrieval logic via a reactive stream.
     *
     * @param ownerId The unique UID of the business owner.
     * @return A real-time [Flow] emitting the list of services or an error.
     */
    operator fun invoke(ownerId: String): Flow<Result<List<Service>>> {
        return serviceRepository.getOwnerServicesStream(ownerId)
    }
}