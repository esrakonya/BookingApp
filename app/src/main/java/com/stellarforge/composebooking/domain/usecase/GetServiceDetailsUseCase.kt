package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.data.repository.AppointmentRepository
import timber.log.Timber
import javax.inject.Inject

class GetServiceDetailsUseCase @Inject constructor(
    private val repository: AppointmentRepository
) {
    suspend operator fun invoke(serviceId: String): Result<Service> {
        return try {
            repository.getServiceDetails(serviceId) // Bu zaten Result döndürüyor
        } catch (e: Exception) {
            // Eğer repository.getServiceDetails kendisi bir exception fırlatırsa
            // (Normalde Result döndürmeli ama garantiye alalım)
            Timber.e("GerServiceDetailsUseCase: Caught unexpected exception: ${e.message}")
            Result.failure(e) // Hatayı Result.failure olarak döndür
        }
    }
}