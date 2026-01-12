package com.stellarforge.composebooking.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Unit tests for [CreateAppointmentUseCase].
 *
 * Verifies:
 * 1. **Validation Logic:** Rejects invalid inputs (Empty name, missing IDs).
 * 2. **Business Logic (CRITICAL):** Ensures 'endTime' is calculated correctly based on duration.
 *    (e.g., 14:00 Start + 45min Duration = 14:45 End).
 *    This is essential for the [GetAvailableSlotsUseCase] overlap logic to work.
 */
class CreateAppointmentUseCaseTest {

    private lateinit var appointmentRepository: AppointmentRepository
    private lateinit var createAppointmentUseCase: CreateAppointmentUseCase

    @Before
    fun setUp() {
        appointmentRepository = mockk(relaxed = true)
        createAppointmentUseCase = CreateAppointmentUseCase(appointmentRepository)
    }

    @Test
    fun `invoke - calculates endTime correctly and passes to repository`() = runTest {
        // GIVEN
        val date = LocalDate.now().plusDays(1)
        val startTime = LocalTime.of(14, 0)
        val serviceDuration = 45

        // Mock successful creation
        coEvery { appointmentRepository.createAppointmentAndSlot(any(), any()) } returns Result.Success(Unit)

        // Capture the 'BookedSlot' object passed to the repository to inspect it
        val slotCaptor = slot<BookedSlot>()

        // WHEN (Eylem)
        val result = createAppointmentUseCase(
            ownerId = "owner1",
            servicePriceInCents = 1000,
            userId = "user1",
            serviceId = "service1",
            serviceName = "Cut",
            serviceDuration = serviceDuration,
            date = date,
            time = startTime,
            customerName = "John",
            customerPhone = "123",
            customerEmail = null
        )

        // THEN
        assertThat(result).isInstanceOf(Result.Success::class.java)

        // Verify repository was called and CAPTURE the arguments
        coVerify {
            appointmentRepository.createAppointmentAndSlot(any(), capture(slotCaptor))
        }

        // Inspect the captured data
        val capturedSlot = slotCaptor.captured

        // Convert captured Firestore Timestamps back to LocalTime for easy verification
        val zoneId = ZoneId.systemDefault()
        val capturedStart = capturedSlot.startTime.toDate().toInstant().atZone(zoneId).toLocalTime()
        val capturedEnd = capturedSlot.endTime.toDate().toInstant().atZone(zoneId).toLocalTime()

        // 1. Start Time should be 14:00
        assertThat(capturedStart.hour).isEqualTo(14)
        assertThat(capturedStart.minute).isEqualTo(0)

        // 2. End Time should be 14:45 (14:00 + 45 mins)
        // This proves the logic inside UseCase is correct
        assertThat(capturedEnd.hour).isEqualTo(14)
        assertThat(capturedEnd.minute).isEqualTo(45)
    }

    @Test
    fun `when customer name is empty, returns Error and DOES NOT call repository`() = runTest {
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
            customerName = "",
            customerPhone = "5551234567",
            customerEmail = null
        )

        // THEN
        assertThat(result).isInstanceOf(Result.Error::class.java)

        // Ensure DB is safe
        coVerify(exactly = 0) { appointmentRepository.createAppointmentAndSlot(any(), any()) }
    }

    @Test
    fun `when required IDs are blank, returns Error`() = runTest {
        // WHEN (Owner ID is missing)
        val result = createAppointmentUseCase(
            ownerId = "",
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