package com.stellarforge.composebooking.data.remote

import com.stellarforge.composebooking.data.model.CustomerProfile
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.flow.Flow

interface CustomerProfileRemoteDataSource {

    /**
     * Updates the user's profile data in Firestore.
     */
    suspend fun updateCustomerProfile(userId: String, name: String, phone: String): Result<Unit>

    fun getCustomerProfileStream(userId: String): Flow<Result<CustomerProfile>>

}