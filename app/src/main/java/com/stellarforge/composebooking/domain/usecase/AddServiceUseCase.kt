package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

class AddServiceUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    /**
     * @param service Firestore'a eklenecek yeni Service nesnesi.
     * @return İşlemin başarısını veya başarısızlığını belirten bir Result.
     */
    suspend operator fun invoke(service: Service): Result<Unit> {
        // UseCase seviyesinde temel iş kurallarını/validasyonları uygulayabiliriz.
        if (service.name.isBlank()) {
            return Result.Error(IllegalArgumentException("Servis adı boş olamaz."))
        }
        if (service.ownerId.isBlank()) {
            return Result.Error(IllegalArgumentException("Servis için bir sahip atanmalıdır."))
        }
        if (service.durationMinutes <= 0) {
            return Result.Error(IllegalArgumentException("Servis süresi 0'dan büyük olmalıdır."))
        }
        // Fiyat negatif olamaz.
        if (service.priceInCents < 0) {
            return Result.Error(IllegalArgumentException("Servis fiyatı negatif olamaz."))
        }

        return serviceRepository.addService(service)
    }
}