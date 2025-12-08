package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

/**
 * UseCase responsible for adding a new [Service] (Product) to the business catalog.
 *
 * **Role:**
 * It acts as a **Validation Layer** before data reaches the repository.
 * It ensures that no incomplete or invalid service data (e.g., negative price, zero duration)
 * is written to the database.
 */
class AddServiceUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    /**
     * Validates the service details and persists them to the remote data source.
     *
     * @param service The [Service] object populated with user input.
     * @return [Result.Success] if saved, or [Result.Error] with an [IllegalArgumentException] if validation fails.
     */
    suspend operator fun invoke(service: Service): Result<Unit> {
        // 1. Validate Name
        if (service.name.isBlank()) {
            return Result.Error(IllegalArgumentException("Service name cannot be empty."))
        }

        // 2. Validate Ownership (Security Check)
        if (service.ownerId.isBlank()) {
            return Result.Error(IllegalArgumentException("Service must have an owner assigned."))
        }

        // 3. Validate Duration (Logic Check)
        if (service.durationMinutes <= 0) {
            return Result.Error(IllegalArgumentException("Service duration must be greater than 0."))
        }

        // 4. Validate Price (Logic Check)
        if (service.priceInCents < 0) {
            return Result.Error(IllegalArgumentException("Service price cannot be negative."))
        }

        // 5. Proceed to Save
        return serviceRepository.addService(service)
    }
}