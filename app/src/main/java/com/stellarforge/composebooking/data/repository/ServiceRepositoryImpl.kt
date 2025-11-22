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

class ServiceRepositoryImpl @Inject constructor(
    private val remoteDataSource: ServiceRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ServiceRepository {
    override fun getOwnerServicesStream(ownerId: String): Flow<Result<List<Service>>> {
        return remoteDataSource.getOwnerServicesStream(ownerId).flowOn(ioDispatcher)
    }

    override suspend fun addService(service: Service): Result<Unit> {
        return withContext(ioDispatcher) { remoteDataSource.addService(service) }
    }

    override suspend fun updateService(service: Service): Result<Unit> {
        return withContext(ioDispatcher) { remoteDataSource.updateService(service) }
    }

    override suspend fun deleteService(serviceId: String): Result<Unit> {
        return withContext(ioDispatcher) { remoteDataSource.deleteService(serviceId) }
    }

    override suspend fun getServiceDetails(serviceId: String): Result<Service> {
        return withContext(ioDispatcher) { remoteDataSource.getServiceDetails(serviceId) }
    }
}