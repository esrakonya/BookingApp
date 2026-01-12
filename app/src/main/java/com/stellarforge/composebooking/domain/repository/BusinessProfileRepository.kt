package com.stellarforge.composebooking.domain.repository

import android.net.Uri
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.flow.Flow

/**
 * Defines the contract for managing the Business Profile (Storefront) data within the Domain layer.
 *
 * This repository is responsible for:
 * 1. Fetching the public-facing details (Name, Address, etc.).
 * 2. Updating profile information.
 * 3. Handling Branding assets (Logo upload).
 */
interface BusinessProfileRepository {

    /**
     * Retrieves the business profile for a specific owner and provides a real-time stream.
     *
     * @param ownerUserId The unique ID of the business owner.
     * @return A [Flow] emitting the profile data or null if not set.
     */
    fun getBusinessProfile(ownerUserId: String): Flow<Result<BusinessProfile?>>

    /**
     * Persists or updates the business profile details.
     *
     * @param ownerUserId The unique ID of the business owner.
     * @param profile The [BusinessProfile] object containing new data.
     */
    suspend fun updateBusinessProfile(ownerUserId: String, profile: BusinessProfile): Result<Unit>

    /**
     * Uploads the business logo to Cloud Storage.
     *
     * @param uri The local URI of the image selected from the gallery.
     * @param ownerId The unique ID of the business owner (used for file naming).
     * @return [Result.Success] containing the **Download URL** (String) if successful.
     */
    suspend fun uploadLogo(uri: Uri, ownerId: String): Result<String>
}