package com.stellarforge.composebooking.data.repository

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.data.remote.ServiceRemoteDataSource
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.di.IoDispatcher
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Concrete implementation of [ServiceRepository] that mediates data operations
 * for Services (Products).
 *
 * **Architecture Role:**
 * It isolates the Domain layer from the details of the Data layer (Firestore).
 * It ensures that all data operations are executed on the appropriate thread ([IoDispatcher]).
 */
class ServiceRepositoryImpl @Inject constructor(
    private val remoteDataSource: ServiceRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ServiceRepository {

    /**
     * Returns a stream of services for the Business Owner.
     * This includes ALL services (Active and Inactive) for management purposes.
     */
    override fun getOwnerServicesStream(ownerId: String): Flow<Result<List<Service>>> {
        return remoteDataSource.getOwnerServicesStream(ownerId).flowOn(ioDispatcher)
    }

    /**
     * Returns a stream of services for Customers.
     * This typically filters the list to show only 'Active' services available for booking.
     */
    override fun getCustomerServicesStream(): Flow<Result<List<Service>>> {
        return remoteDataSource.getCustomerServicesStream().flowOn(ioDispatcher)
    }

    override suspend fun addService(service: Service): Result<Unit> {
        return withContext(ioDispatcher) {
            remoteDataSource.addService(service)
        }
    }

    override suspend fun updateService(service: Service): Result<Unit> {
        return withContext(ioDispatcher) {
            remoteDataSource.updateService(service)
        }
    }

    override suspend fun deleteService(serviceId: String): Result<Unit> {
        return withContext(ioDispatcher) {
            remoteDataSource.deleteService(serviceId)
        }
    }

    override suspend fun getServiceDetails(serviceId: String): Result<Service> {
        return withContext(ioDispatcher) {
            remoteDataSource.getServiceDetails(serviceId)
        }
    }
}