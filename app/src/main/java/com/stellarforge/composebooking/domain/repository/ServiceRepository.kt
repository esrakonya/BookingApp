package com.stellarforge.composebooking.domain.repository

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.flow.Flow

/**
 * Sadece servislerle ilgili veri işlemlerini yöneten arayüz.
 */
interface ServiceRepository {
    fun getOwnerServicesStream(ownerId: String): Flow<Result<List<Service>>>
    suspend fun addService(service: Service): Result<Unit>
    suspend fun updateService(service: Service): Result<Unit>
    suspend fun deleteService(serviceId: String): Result<Unit>
    suspend fun getServiceDetails(serviceId: String): Result<Service>
}