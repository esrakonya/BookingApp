package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.utils.mapOnSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.firebase.Timestamp
import com.stellarforge.composebooking.data.model.Appointment
import java.util.Date
import javax.inject.Inject

/**
 * "Randevularım" ekranı için işlenmiş verileri sağlayan UseCase.
 *
 * Bu UseCase, belirli bir kullanıcıya ait tüm randevuları getirir ve onları
 * "Gelecek" ve "Geçmiş" randevular olarak iki gruba ayırır.
 */
class GetMyBookingsUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) {
    /**
     * @param userId Randevuları listelenecek müşterinin UID'si.
     * @return Gelecek ve Geçmiş randevuları ayıran [MyBookings] data class'ını
     *         veya bir hatayı içeren bir Flow.
     */
    operator fun invoke(userId: String): Flow<Result<MyBookings>> {
        return appointmentRepository.getMyBookingsStream(userId)
            .map { result ->
                result.mapOnSuccess { bookings ->
                    val now = Timestamp(Date())

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

data class MyBookings(
    val upcomingBookings: List<Appointment>,
    val pastBookings: List<Appointment>
)