package com.stellarforge.composebooking.data.remote

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for raw data source operations related to [Service] data.
 *
 * This interface abstracts the underlying database interactions (e.g., Firestore)
 * regarding the "Products/Services" offered by the business.
 *
 * It distinguishes between:
 * 1. **Owner View:** Needs to see all services (Active/Inactive) to manage them.
 * 2. **Customer View:** Should only see 'Active' services to book them.
 */
interface ServiceRemoteDataSource {
    /**
     * Returns a real-time stream (Flow) of services owned by a specific user.
     *
     * @param ownerId The UID of the business owner.
     * @return A Flow containing ALL services (Active and Inactive) for the admin panel.
     */
    fun getOwnerServicesStream(ownerId: String): Flow<Result<List<Service>>>

    /**
     * Returns a real-time stream of services visible to customers.
     *
     * Unlike [getOwnerServicesStream], this typically returns a filtered list
     * (e.g., only `isActive = true`) representing the public storefront.
     */
    fun getCustomerServicesStream(): Flow<Result<List<Service>>>

    /**
     * Persists a new [Service] to the remote database.
     */
    suspend fun addService(service: Service): Result<Unit>

    /**
     * Updates the details of an existing [Service] in the remote database.
     */
    suspend fun updateService(service: Service): Result<Unit>

    /**
     * Permanently deletes a service from the remote database using its unique ID.
     */
    suspend fun deleteService(serviceId: String): Result<Unit>

    /**
     * Fetches the full details of a specific service.
     * Useful for the 'Edit Service' screen or the 'Booking' flow initialization.
     */
    suspend fun getServiceDetails(serviceId: String): Result<Service>
}

