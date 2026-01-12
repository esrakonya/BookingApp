package com.stellarforge.composebooking.data.repository

import com.stellarforge.composebooking.data.remote.SlotRemoteDataSource
import com.stellarforge.composebooking.utils.Result
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class SlotRepositoryImplTest {

    private val dataSource: SlotRemoteDataSource = mockk()
    private val ioDispatcher = Dispatchers.Unconfined
    private lateinit var repository: SlotRepositoryImpl

    @Before
    fun setup() {
        repository = SlotRepositoryImpl(dataSource, ioDispatcher)
    }

    @Test
    fun `getSlotsForDate - delegates to dataSource`() = runBlocking {
        coEvery { dataSource.getSlotsForDate(any(), any()) } returns Result.Success(emptyList())

        repository.getSlotsForDate("owner1", LocalDate.now())

        coVerify(exactly = 1) { dataSource.getSlotsForDate(any(), any()) }
    }
}