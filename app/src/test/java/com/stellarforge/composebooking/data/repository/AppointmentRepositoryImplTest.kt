package com.stellarforge.composebooking.data.repository

import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.remote.AppointmentRemoteDataSource
import com.stellarforge.composebooking.utils.Result
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class AppointmentRepositoryImplTest {

    private val dataSource: AppointmentRemoteDataSource = mockk()
    private val ioDispatcher = Dispatchers.Unconfined
    private lateinit var repository: AppointmentRepositoryImpl

    @Before
    fun setup() {
        repository = AppointmentRepositoryImpl(dataSource, ioDispatcher)
    }

    @Test
    fun `getAppointmentsForDate - delegates to dataSource`() = runBlocking {
        coEvery { dataSource.getAppointmentsForDate(any(), any()) } returns Result.Success(emptyList())

        repository.getAppointmentsForDate("owner1", LocalDate.now())

        coVerify(exactly = 1) { dataSource.getAppointmentsForDate(any(), any()) }
    }

    @Test
    fun `getMyBookingsStream - returns flow from dataSource`() = runBlocking {
        every { dataSource.getMyBookingsStream("user1") } returns flowOf(Result.Success(emptyList()))

        repository.getMyBookingsStream("user1")

        verify(exactly = 1) { dataSource.getMyBookingsStream("user1") }
    }
}