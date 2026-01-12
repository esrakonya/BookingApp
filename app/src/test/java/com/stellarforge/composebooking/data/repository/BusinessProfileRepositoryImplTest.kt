package com.stellarforge.composebooking.data.repository

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestore
import com.stellarforge.composebooking.data.remote.BusinessProfileRemoteDataSource
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.data.model.BusinessProfile
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class BusinessProfileRepositoryImplTest {

    private val dataSource: BusinessProfileRemoteDataSource = mockk()
    private val firestore: FirebaseFirestore = mockk(relaxed = true)
    private val ioDispatcher = Dispatchers.Unconfined
    private lateinit var repository: BusinessProfileRepositoryImpl

    @Before
    fun setup() {
        repository = BusinessProfileRepositoryImpl(dataSource, firestore, ioDispatcher)
    }

    @Test
    fun `updateBusinessProfile - delegates to dataSource`() = runBlocking {
        val profile = BusinessProfile(businessName = "Test Shop")
        coEvery { dataSource.updateBusinessProfile("owner1", profile) } returns Result.Success(Unit)

        repository.updateBusinessProfile("owner1", profile)

        coVerify(exactly = 1) { dataSource.updateBusinessProfile("owner1", profile) }
    }

    @Test
    fun `uploadLogo - delegates to dataSource`() = runBlocking {
        // ARRANGE
        val uri = mockk<Uri>()
        val ownerId = "owner_1"
        val expectedUrl = "http://logo.url"

        coEvery { dataSource.uploadLogo(uri, ownerId) } returns Result.Success(expectedUrl)

        // ACT
        val result = repository.uploadLogo(uri, ownerId)

        // ASSERT
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(expectedUrl)

        coVerify(exactly = 1) { dataSource.uploadLogo(uri, ownerId) }
    }
}