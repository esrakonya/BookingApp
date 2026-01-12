package com.stellarforge.composebooking.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.stellarforge.composebooking.data.model.CustomerProfile
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class CustomerProfileRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CustomerProfileRemoteDataSource {

    override suspend fun updateCustomerProfile(
        userId: String,
        name: String,
        phone: String
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "name" to name,
                "phone" to phone
            )
            firestore.collection(FirebaseConstants.USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update user profile: $userId")
            Result.Error(e)
        }
    }

    override fun getCustomerProfileStream(userId: String): Flow<Result<CustomerProfile>> = callbackFlow {
        val listener = firestore.collection(FirebaseConstants.USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val profile = snapshot.toObject(CustomerProfile::class.java)?.copy(id = snapshot.id)
                        if (profile != null) {
                            trySend(Result.Success(profile))
                        }
                    } catch (e: Exception) {
                        trySend(Result.Error(e))
                    }
                }
            }
        awaitClose { listener.remove() }
    }
}