package com.stellarforge.composebooking.data.remote

import android.net.Uri
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.utils.Result

/**
 * Defines the contract for managing the public-facing Business Profile data remotely.
 *
 * This interface handles operations related to the "Storefront" information
 * (e.g., Business Name, Address, Contact Info) that is displayed to customers.
 * Implementations of this interface (e.g., using Firestore) are responsible for
 * persisting this data to the cloud.
 */
interface BusinessProfileRemoteDataSource {

    /**
     * Writes or updates the business profile in the remote database.
     *
     * This operation is typically an "Upsert" (Update or Insert): if the document
     * for the given [ownerUserId] does not exist, it creates it; otherwise, it overwrites it.
     *
     * @param ownerUserId The unique identifier of the business owner (used as Document ID).
     * @param profile The [BusinessProfile] data object containing the new details.
     */
    suspend fun updateBusinessProfile(ownerUserId: String, profile: BusinessProfile): Result<Unit>

    suspend fun uploadLogo(uri: Uri, ownerId: String): Result<String>
}