package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

/**
 * UseCase responsible for permanently deleting a [Service] (Product) from the catalog.
 *
 * **Business Logic Note:**
 * Currently, this operation performs a direct deletion. In a more complex production environment,
 * you might want to extend this UseCase to check for upcoming appointments linked to this service
 * and prevent deletion if conflicts exist (Referential Integrity).
 */
class DeleteServiceUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    /**
     * Executes the deletion logic.
     *
     * @param serviceId The unique Firestore document ID of the service to be deleted.
     * @return [Result.Success] if deleted, or [Result.Error] if validation fails or DB error occurs.
     */
    suspend operator fun invoke(serviceId: String): Result<Unit> {
        // 1. Validation: Fail fast on invalid ID
        if (serviceId.isBlank()) {
            return Result.Error(IllegalArgumentException("Service ID cannot be blank for deletion."))
        }

        // 2. Execution
        return serviceRepository.deleteService(serviceId)
    }
}