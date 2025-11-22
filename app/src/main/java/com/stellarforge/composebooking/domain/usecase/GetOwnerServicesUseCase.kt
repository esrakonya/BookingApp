package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Belirli bir işletme sahibine ait tüm servisleri (aktif ve pasif)
 * anlık larak dinleyen UseCase
 */
class GetOwnerServicesUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    /**
     * @param ownerId Servisleri listelenecek işletme sahibinin UID'si.
     * @return Servis listesini veya hatayı içeren bir Flow.
     */
    operator fun invoke(ownerId: String): Flow<Result<List<Service>>> {
        return serviceRepository.getOwnerServicesStream(ownerId)
    }
}