package com.stellarforge.composebooking.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for [CreateAppointmentUseCase].
 *
 * Focuses on VALIDATION rules before interacting with the database.
 * Ensures that incomplete or invalid data is rejected early to save server resources.
 */
class CreateAppointmentUseCaseTest {

    private lateinit var appointmentRepository: AppointmentRepository
    private lateinit var createAppointmentUseCase: CreateAppointmentUseCase

    @Before
    fun setUp() {
        appointmentRepository = mockk()
        createAppointmentUseCase = CreateAppointmentUseCase(appointmentRepository)
    }

    @Test
    fun `when inputs are valid, repository create function is called`() = runTest {
        // GIVEN (Setup)
        coEvery { appointmentRepository.createAppointmentAndSlot(any(), any()) } returns Result.Success(Unit)

        // WHEN (Action)
        val result = createAppointmentUseCase(
            ownerId = "owner1",
            servicePriceInCents = 1000,
            userId = "user1",
            serviceId = "service1",
            serviceName = "Manicure",
            serviceDuration = 30,
            date = LocalDate.now().plusDays(1), // Future date
            time = LocalTime.of(14, 0),
            customerName = "John Doe", // Valid Name
            customerPhone = "5551234567", // Valid Phone
            customerEmail = "test@test.com"
        )

        // THEN (Assertion)
        assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify(exactly = 1) { appointmentRepository.createAppointmentAndSlot(any(), any()) }
    }

    @Test
    fun `when customer name is empty, returns Error and DOES NOT call repository`() = runTest {
        // GIVEN
        // No repository stubbing needed as we expect early termination.

        // WHEN
        val result = createAppointmentUseCase(
            ownerId = "owner1",
            servicePriceInCents = 1000,
            userId = "user1",
            serviceId = "service1",
            serviceName = "Manicure",
            serviceDuration = 30,
            date = LocalDate.now().plusDays(1),
            time = LocalTime.of(14, 0),
            customerName = "", // <--- ERROR: Empty Name
            customerPhone = "5551234567",
            customerEmail = null
        )

        // THEN
        assertThat(result).isInstanceOf(Result.Error::class.java)

        // CRITICAL: Ensure database is protected from invalid data.
        coVerify(exactly = 0) { appointmentRepository.createAppointmentAndSlot(any(), any()) }
    }

    @Test
    fun `when required IDs are blank, returns Error`() = runTest {
        // WHEN (Owner ID is missing)
        val result = createAppointmentUseCase(
            ownerId = "", // <--- ERROR
            servicePriceInCents = 1000,
            userId = "user1",
            serviceId = "service1",
            serviceName = "Manicure",
            serviceDuration = 30,
            date = LocalDate.now(),
            time = LocalTime.of(14, 0),
            customerName = "John Doe",
            customerPhone = "555",
            customerEmail = null
        )

        // THEN
        assertThat(result).isInstanceOf(Result.Error::class.java)
        coVerify(exactly = 0) { appointmentRepository.createAppointmentAndSlot(any(), any()) }
    }
}