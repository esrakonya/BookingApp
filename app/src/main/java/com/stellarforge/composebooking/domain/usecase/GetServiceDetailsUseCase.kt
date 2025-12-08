package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.utils.DocumentNotFoundException
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

/**
 * UseCase responsible for retrieving the full details of a specific [Service].
 *
 * **Usage Context:**
 * - When an Owner clicks "Edit" on a service card.
 * - When a Customer clicks a service to view details or book an appointment.
 *
 * **Error Handling Strategy:**
 * This UseCase implements a "Graceful Failure" pattern for missing documents.
 * If a service ID provided does not exist in the database (e.g., deleted by another admin),
 * it returns `Success(null)` instead of throwing an `Error`.
 * This allows the UI to handle the "Not Found" state explicitly (e.g., show a toast or close screen)
 * rather than treating it as a generic crash.
 */
class GetServiceDetailsUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    /**
     * Executes the retrieval logic.
     *
     * @param serviceId The unique ID of the service to fetch.
     * @return
     * - [Result.Success] with [Service] data if found.
     * - [Result.Success] with `null` if the document does not exist.
     * - [Result.Error] for other system/network failures.
     */
    suspend operator fun invoke(serviceId: String): Result<Service?> {
        // 1. Validate Input
        if (serviceId.isBlank()) {
            return Result.Error(IllegalArgumentException("Service ID cannot be blank."))
        }

        // 2. Fetch from Repository
        when (val result = serviceRepository.getServiceDetails(serviceId)) {
            is Result.Success -> {
                return Result.Success(result.data)
            }

            is Result.Error -> {
                // 3. Special handling for "Not Found" exception
                return if (result.exception is DocumentNotFoundException) {
                    Result.Success(null)
                } else {
                    Result.Error(result.exception)
                }
            }

            is Result.Loading -> return Result.Loading
        }
    }
}