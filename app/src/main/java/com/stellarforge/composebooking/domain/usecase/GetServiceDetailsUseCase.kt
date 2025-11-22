package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.utils.DocumentNotFoundException
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

class GetServiceDetailsUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    /**
     * @return Servis detayını, bulunamazsa null içeren bir Result.
     *         "Bulunamadı" durumu `Success(null)` olarak, diğer hatalar `Error` olarak döner.
     */
    suspend operator fun invoke(serviceId: String): Result<Service?> {
        if (serviceId.isBlank()) {
            return Result.Error(IllegalArgumentException("Service ID cannot be blank."))
        }

        when (val result = serviceRepository.getServiceDetails(serviceId)) {
            is Result.Success -> {
                return Result.Success(result.data)
            }

            is Result.Error -> {
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