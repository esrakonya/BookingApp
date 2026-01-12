package com.stellarforge.composebooking.data.repository

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.CustomerProfile
import com.stellarforge.composebooking.data.remote.CustomerProfileRemoteDataSource
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [UserRepositoryImpl].
 *
 * Verifies that the Repository correctly delegates calls to the DataSource
 * and handles data flow.
 */
class CustomerProfileRepositoryImplTest {

    // Mock the DataSource (The lower layer)
    private val customerProfileRemoteDataSource: CustomerProfileRemoteDataSource = mockk()

    // Use Unconfined dispatcher to execute coroutines immediately in tests
    private val ioDispatcher = Dispatchers.Unconfined

    private lateinit var repository: CustomerProfileRepositoryImpl

    @Before
    fun setup() {
        repository = CustomerProfileRepositoryImpl(customerProfileRemoteDataSource, ioDispatcher)
    }

    @Test
    fun `updateUserProfile - delegates to dataSource successfully`() = runBlocking {
        // ARRANGE
        val userId = "123"
        val name = "New Name"
        val phone = "999"

        // Mock successful response from DataSource
        coEvery {
            customerProfileRemoteDataSource.updateCustomerProfile(userId, name, phone)
        } returns Result.Success(Unit)

        // ACT
        val result = repository.updateCustomerProfile(userId, name, phone)

        // ASSERT
        assertThat(result).isInstanceOf(Result.Success::class.java)

        // Verify: Did the repository actually call the data source?
        coVerify(exactly = 1) {
            customerProfileRemoteDataSource.updateCustomerProfile(userId, name, phone)
        }
    }

    @Test
    fun `getUserProfileStream - delegates flow to dataSource`() = runBlocking {
        // ARRANGE
        val userId = "123"
        val testProfile = CustomerProfile(id = "123", name = "Test User")

        // Mock the flow
        every {
            customerProfileRemoteDataSource.getCustomerProfileStream(userId)
        } returns flowOf(Result.Success(testProfile))

        // ACT
        val flow = repository.getCustomerProfileStream(userId)

        // ASSERT
        // We verify the interaction happened
        verify(exactly = 1) { customerProfileRemoteDataSource.getCustomerProfileStream(userId) }
    }
}