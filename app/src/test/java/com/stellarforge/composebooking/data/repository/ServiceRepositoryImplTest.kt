package com.stellarforge.composebooking.data.repository

import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.data.remote.ServiceRemoteDataSource
import com.stellarforge.composebooking.utils.Result
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class ServiceRepositoryImplTest {

    private val dataSource: ServiceRemoteDataSource = mockk()
    private val ioDispatcher = Dispatchers.Unconfined
    private lateinit var repository: ServiceRepositoryImpl

    @Before
    fun setup() {
        repository = ServiceRepositoryImpl(dataSource, ioDispatcher)
    }

    @Test
    fun `addService - delegates to dataSource`() = runBlocking {
        val service = Service(name = "Test Service")
        coEvery { dataSource.addService(service) } returns Result.Success(Unit)

        repository.addService(service)

        coVerify(exactly = 1) { dataSource.addService(service) }
    }

    @Test
    fun `deleteService - delegates to dataSource`() = runBlocking {
        coEvery { dataSource.deleteService("123") } returns Result.Success(Unit)

        repository.deleteService("123")

        coVerify(exactly = 1) { dataSource.deleteService("123") }
    }
}