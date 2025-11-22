package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

class UpdateServiceUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    /**
     * @param service Güncellenmiş verileri içeren Service nesnesi.
     * @return İşlemin başarısını veya başarısızlığını belirten bir Result.
     */
    suspend operator fun invoke(service: Service): Result<Unit> {
        // Güncelleme için ID'nin var olması zorunludur.
        if (service.id.isBlank()) {
            return Result.Error(IllegalArgumentException("Güncellenecek servisin bir ID'si olmalıdır."))
        }
        if (service.name.isBlank()) {
            return Result.Error(IllegalArgumentException("Servis adı boş olamaz."))
        }
        if (service.durationMinutes <= 0) {
            return Result.Error(IllegalArgumentException("Servis süresi 0'dan büyük olmalıdır."))
        }
        if (service.priceInCents < 0) {
            return Result.Error(IllegalArgumentException("Servis fiyatı negatif olamaz."))
        }

        return serviceRepository.updateService(service)
    }
}