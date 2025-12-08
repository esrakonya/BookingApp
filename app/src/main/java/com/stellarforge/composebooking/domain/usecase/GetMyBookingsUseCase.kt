package com.stellarforge.composebooking.domain.usecase

import com.google.firebase.Timestamp
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.utils.mapOnSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

/**
 * UseCase responsible for retrieving and organizing the user's booking history.
 *
 * **Data Transformation Logic:**
 * Instead of returning a raw list to the UI, this UseCase processes the stream to separate
 * appointments into **"Upcoming"** and **"Past"** categories based on the current timestamp.
 *
 * This simplifies the UI logic significantly, allowing the View/ViewModel to just render
 * the lists without performing date calculations.
 */
class GetMyBookingsUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) {
    /**
     * Retrieves the user's bookings and partitions them based on the current time.
     *
     * @param userId The UID of the customer.
     * @return A [Flow] emitting [Result] which contains the structured [MyBookings] object.
     */
    operator fun invoke(userId: String): Flow<Result<MyBookings>> {
        return appointmentRepository.getMyBookingsStream(userId)
            .map { result ->
                // Helper function to transform data only if the Result is Success
                result.mapOnSuccess { bookings ->
                    val now = Timestamp(Date())

                    // Efficiently splits the list into two based on the predicate
                    // partition() is faster and cleaner than filtering twice.
                    val (upcoming, past) = bookings.partition {
                        it.appointmentDateTime >= now
                    }

                    MyBookings(
                        upcomingBookings = upcoming,
                        pastBookings = past
                    )
                }
            }
    }
}

/**
 * A Wrapper Model (UI Model) to hold partitioned booking lists.
 * This eliminates the need for the UI to filter the list itself.
 */
data class MyBookings(
    val upcomingBookings: List<Appointment>,
    val pastBookings: List<Appointment>
)