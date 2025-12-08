package com.stellarforge.composebooking.domain.repository

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for managing [Service] (Product) data within the Domain layer.
 *
 * This repository is the source of truth for the services offered by the business.
 * It strictly separates the "Management View" (for Owners) from the "Storefront View" (for Customers).
 */
interface ServiceRepository {

    /**
     * Returns a real-time stream of ALL services owned by the user.
     *
     * This includes both **Active** and **Inactive** services, allowing the business owner
     * to manage their full catalog (edit, delete, toggle visibility).
     *
     * @param ownerId The UID of the business owner.
     */
    fun getOwnerServicesStream(ownerId: String): Flow<Result<List<Service>>>

    /**
     * Returns a real-time stream of services available for booking.
     *
     * Unlike the owner stream, this typically returns a filtered list containing
     * only **Active** services. This represents the public-facing menu.
     */
    fun getCustomerServicesStream(): Flow<Result<List<Service>>>

    /**
     * Persists a new service to the database.
     */
    suspend fun addService(service: Service): Result<Unit>

    /**
     * Updates an existing service's details (Name, Price, Duration, etc.).
     */
    suspend fun updateService(service: Service): Result<Unit>

    /**
     * Permanently removes a service from the catalog.
     */
    suspend fun deleteService(serviceId: String): Result<Unit>

    /**
     * Retrieves the full details of a specific service.
     * Useful for the "Edit Service" screen or populating booking details.
     */
    suspend fun getServiceDetails(serviceId: String): Result<Service>
}