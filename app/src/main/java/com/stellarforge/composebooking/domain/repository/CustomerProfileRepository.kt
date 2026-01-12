package com.stellarforge.composebooking.domain.repository

import com.stellarforge.composebooking.data.model.CustomerProfile
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.flow.Flow

interface CustomerProfileRepository {

    suspend fun updateCustomerProfile(userId: String, name: String, phone: String): Result<Unit>

    fun getCustomerProfileStream(userId: String): Flow<Result<CustomerProfile>>
}