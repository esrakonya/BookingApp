package com.stellarforge.composebooking.domain.repository

import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for managing the Business Profile (Storefront) data within the Domain layer.
 *
 * This repository is responsible for fetching and updating the public-facing details of the business
 * (e.g., Business Name, Address, Contact Info) that are displayed to customers.
 */
interface BusinessProfileRepository {

    /**
     * Retrieves the business profile for a specific owner and provides a real-time stream via [Flow].
     *
     * This stream reflects changes instantly. For example, if the owner changes the shop name,
     * the customer app updates immediately without refreshing.
     *
     * @param ownerUserId The unique ID of the business owner.
     * @return A [Flow] emitting [Result]:
     *         - [Result.Success] containing [BusinessProfile] if found.
     *         - [Result.Success] containing `null` if the profile document does not exist yet (New Business).
     *         - [Result.Error] if the network request fails.
     */
    fun getBusinessProfile(ownerUserId: String): Flow<Result<BusinessProfile?>>

    /**
     * Persists or updates the business profile details.
     *
     * This operation performs an "Upsert" (Update or Insert) strategy in the data layer.
     *
     * @param ownerUserId The unique ID of the business owner.
     * @param profile The [BusinessProfile] object containing the new data.
     * @return [Result.Success] if the write operation completes, or [Result.Error] otherwise.
     */
    suspend fun updateBusinessProfile(ownerUserId: String, profile: BusinessProfile): Result<Unit>
}