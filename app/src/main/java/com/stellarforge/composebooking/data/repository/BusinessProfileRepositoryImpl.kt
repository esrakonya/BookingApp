package com.stellarforge.composebooking.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.data.remote.BusinessProfileRemoteDataSource
import com.stellarforge.composebooking.di.IoDispatcher
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

import javax.inject.Inject

class BusinessProfileRepositoryImpl @Inject constructor(
    private val remoteDataSource: BusinessProfileRemoteDataSource,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BusinessProfileRepository {
    override fun getBusinessProfile(ownerUserId: String): Flow<Result<BusinessProfile?>> {
        Timber.d("Repository: Requesting business profile snapshots for ownerUserId: $ownerUserId")
        return firestore.collection(FirebaseConstants.BUSINESSES_COLLECTION)
            .document(ownerUserId)
            .snapshots()
            .map<DocumentSnapshot, Result<BusinessProfile?>> { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    try {
                        val profile = documentSnapshot.toObject(BusinessProfile::class.java)
                        if (profile != null) {
                            Timber.d("Repository: Profile found and mapped for $ownerUserId: $profile")
                            Result.Success(profile)
                        } else {
                            Timber.w("Repository: Profile document exists but toObject returned null for $ownerUserId")
                            Result.Error(
                                Exception("Failed to parse business profile data from Firestore."),
                                "Could not read profile information."
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Repository: Error parsing business profile for $ownerUserId during map")
                        Result.Error(
                            Exception("Error parsing business profile: ${e.localizedMessage}", e),
                            "An error occurred while reading profile data."
                        )
                    }
                } else {
                    Timber.d("Repository: No business profile document found for $ownerUserId")
                    Result.Success(null)
                }
            }
            .catch { exception ->
                Timber.e(exception, "Repository: Error in getBusinessProfile snapshots flow for $ownerUserId")
                emit(Result.Error(
                    Exception("Failed to get business profile updates: ${exception.localizedMessage}", exception),
                    "Could not fetch profile updates."
                ))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun updateBusinessProfile(
        ownerUserId: String,
        profile: BusinessProfile
    ): Result<Unit> {
        Timber.d("Repository: Attempting to update business profile for ownerUserId: $ownerUserId")
        return withContext(ioDispatcher) {
            remoteDataSource.updateBusinessProfile(ownerUserId, profile)
        }
    }

}