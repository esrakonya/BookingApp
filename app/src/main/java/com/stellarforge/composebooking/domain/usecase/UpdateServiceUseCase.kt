package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

/**
 * UseCase responsible for modifying the details of an existing [Service] in the catalog.
 *
 * **Validation Logic:**
 * Similar to [AddServiceUseCase], it enforces data integrity rules (Name, Price, Duration).
 * Additionally, it strictly requires a valid **Service ID** to ensure the correct document is updated.
 */
class UpdateServiceUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    /**
     * Validates the updated service data and persists changes to the remote data source.
     *
     * @param service The [Service] object containing the modified data and the original ID.
     * @return [Result.Success] if updated, or [Result.Error] if validation fails.
     */
    suspend operator fun invoke(service: Service): Result<Unit> {
        // 1. Critical Validation: ID must exist for an update operation
        if (service.id.isBlank()) {
            return Result.Error(IllegalArgumentException("Service ID is required for updates."))
        }

        // 2. Validate Name
        if (service.name.isBlank()) {
            return Result.Error(IllegalArgumentException("Service name cannot be empty."))
        }

        // 3. Validate Duration
        if (service.durationMinutes <= 0) {
            return Result.Error(IllegalArgumentException("Service duration must be greater than 0."))
        }

        // 4. Validate Price
        if (service.priceInCents < 0) {
            return Result.Error(IllegalArgumentException("Service price cannot be negative."))
        }

        // 5. Execute Update
        return serviceRepository.updateService(service)
    }
}