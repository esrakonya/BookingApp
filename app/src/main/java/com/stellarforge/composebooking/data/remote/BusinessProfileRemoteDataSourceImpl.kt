package com.stellarforge.composebooking.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class BusinessProfileRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : BusinessProfileRemoteDataSource {
    override suspend fun updateBusinessProfile(
        ownerUserId: String,
        profile: BusinessProfile
    ): Result<Unit> {
        Timber.d("DataSource: Updating business profile for ownerId: $ownerUserId")
        return try {
            firestore.collection(FirebaseConstants.BUSINESSES_COLLECTION)
                .document(ownerUserId)
                .set(profile)
                .await()
            Timber.i("DataSource: Business profile successfully updated for ownerId: $ownerUserId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "DataSource: Error updating business profile for ownerId: $ownerUserId")
            Result.Error(e, "Failed to update business profile: ${e.localizedMessage}")
        }
    }

}