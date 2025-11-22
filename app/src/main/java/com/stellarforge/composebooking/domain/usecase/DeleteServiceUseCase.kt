package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

/**
 * Belirtilen ID'ye sahip bir servisi silen UseCase
 */
class DeleteServiceUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    /**
     * @param serviceId Silinecek servisin Firestore döküman ID'si.
     * @return İşlemin başarısını veya başarısızlığını belirten bir Result.
     */
    suspend operator fun invoke(serviceId: String): Result<Unit> {
        // ID'nin boş olmadığından emin olalım (ek bir güvenlik katmanı).
        if (serviceId.isBlank()) {
            return Result.Error(IllegalArgumentException("Service ID cannot be blank for deletion."))
        }

        // Gelecekte eklenebilecek iş mantığı:
        // 1. Bu servise ait yaklaşan randevuları kontrol et.
        // 2. Varsa, silmeye izin verme ve kullanıcıya bir hata mesajı göster.
        // Şimdilik doğrudan silme işlemini çağırıyoruz.
        return serviceRepository.deleteService(serviceId)
    }
}