package com.stellarforge.composebooking.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.data.remote.BusinessProfileRemoteDataSource
import com.stellarforge.composebooking.di.IoDispatcher
import com.stellarforge.composebooking.domain.repository.BusinessProfileRepository
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

/**
 * Concrete implementation of [BusinessProfileRepository].
 *
 * This repository handles the data flow for the Business Profile (Storefront).
 *
 * **Key Features:**
 * - **Reactive Streams:** Uses Firestore `snapshots()` to provide a live [Flow].
 *   If the owner updates the shop name, all customers see the change instantly.
 * - **Error Handling:** Wraps Firestore exceptions into our custom [Result] type.
 * - **Dispatcher Safety:** Ensures heavy mapping operations run on the IO thread.
 */
class BusinessProfileRepositoryImpl @Inject constructor(
    private val remoteDataSource: BusinessProfileRemoteDataSource,
    private val firestore: FirebaseFirestore, // Used directly for the snapshot stream
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BusinessProfileRepository {

    /**
     * Returns a live stream of the business profile.
     *
     * It listens to the document in real-time. If the document doesn't exist (yet),
     * it emits `Success(null)` so the UI can show a default state instead of an error.
     */
    override fun getBusinessProfile(ownerUserId: String): Flow<Result<BusinessProfile?>> {
        Timber.d("Repository: Requesting live profile updates for ownerId: $ownerUserId")

        return firestore.collection(FirebaseConstants.BUSINESSES_COLLECTION)
            .document(ownerUserId)
            .snapshots()
            .map<DocumentSnapshot, Result<BusinessProfile?>> { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    try {
                        val profile = documentSnapshot.toObject(BusinessProfile::class.java)
                        if (profile != null) {
                            // Timber.d("Repository: Profile update received: ${profile.businessName}")
                            Result.Success(profile)
                        } else {
                            Timber.w("Repository: Document exists but parsing returned null.")
                            Result.Error(
                                Exception("Data Parsing Error"),
                                "Failed to read profile details."
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Repository: Exception during profile mapping.")
                        Result.Error(e, "An error occurred while processing data.")
                    }
                } else {
                    Timber.i("Repository: No profile document found (New Business).")
                    Result.Success(null)
                }
            }
            .catch { exception ->
                Timber.e(exception, "Repository: Stream interruption.")
                emit(Result.Error(
                    Exception("Stream Error", exception),
                    "Could not fetch profile updates."
                ))
            }
            .flowOn(ioDispatcher)
    }

    /**
     * Delegates the update operation to the RemoteDataSource.
     */
    override suspend fun updateBusinessProfile(
        ownerUserId: String,
        profile: BusinessProfile
    ): Result<Unit> {
        Timber.d("Repository: Updating profile...")
        return withContext(ioDispatcher) {
            remoteDataSource.updateBusinessProfile(ownerUserId, profile)
        }
    }
}