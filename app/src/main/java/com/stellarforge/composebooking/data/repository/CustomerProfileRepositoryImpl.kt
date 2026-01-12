package com.stellarforge.composebooking.data.repository

import com.stellarforge.composebooking.data.model.CustomerProfile
import com.stellarforge.composebooking.data.remote.CustomerProfileRemoteDataSource
import com.stellarforge.composebooking.di.IoDispatcher
import com.stellarforge.composebooking.domain.repository.CustomerProfileRepository
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Concrete implementation of [CustomerProfileRepository].
 *
 * **Responsibilities:**
 * - Acts as the single source of truth for Customer Profile data.
 * - Coordinates data flow between the Domain Layer (UseCases) and the Data Layer (RemoteDataSource).
 * - **Threading:** Ensures that heavy network operations (writes) are offloaded to the [IoDispatcher],
 *   keeping the UI (Main Thread) responsive.
 */
class CustomerProfileRepositoryImpl @Inject constructor(
    private val customerProfileRemoteDataSource: CustomerProfileRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CustomerProfileRepository {

    /**
     * Updates the customer's personal details (Name, Phone) in the remote database.
     *
     * This function is **Main-Safe**. It switches to the IO context internally,
     * so it can be safely called from the ViewModel without freezing the UI.
     */
    override suspend fun updateCustomerProfile(
        userId: String,
        name: String,
        phone: String
    ): Result<Unit> {
        return withContext(ioDispatcher) {
            customerProfileRemoteDataSource.updateCustomerProfile(userId, name, phone)
        }
    }

    /**
     * Provides a real-time stream (`Flow`) of the customer's profile data.
     *
     * **Real-time Logic:**
     * Unlike a one-time fetch, this Flow emits a new value whenever the data changes in Firestore.
     * This ensures that if the user updates their profile, the "My Account" screen reflects
     * the changes immediately without needing a manual refresh.
     */
    override fun getCustomerProfileStream(userId: String): Flow<Result<CustomerProfile>> {
        // Flows are cold and run in the collector's context (usually ViewModelScope),
        // but the DataSource implementation handles the underlying listener.
        return customerProfileRemoteDataSource.getCustomerProfileStream(userId)
    }
}