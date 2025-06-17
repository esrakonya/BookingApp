package com.stellarforge.composebooking.data.remote

import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.utils.Result

interface BusinessProfileRemoteDataSource {

    /**
     * İşletme profilini Firestore'a yazar veya günceller.
     */
    suspend fun updateBusinessProfile(ownerUserId: String, profile: BusinessProfile): Result<Unit>
}