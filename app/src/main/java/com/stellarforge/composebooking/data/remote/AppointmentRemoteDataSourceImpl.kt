package com.stellarforge.composebooking.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

class AppointmentRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AppointmentRemoteDataSource {

    private val appointmentsCollection: CollectionReference = firestore.collection(FirebaseConstants.APPOINTMENTS_COLLECTION)
    private val slotsCollection: CollectionReference = firestore.collection(FirebaseConstants.BOOKED_SLOTS_COLLECTION)

    override suspend fun getAppointmentsForDate(
        ownerId: String,
        date: LocalDate
    ): Result<List<Appointment>> {
        return try {
            val zoneId = ZoneId.systemDefault()
            val startInstant = date.atStartOfDay(zoneId).toInstant()
            val endInstant = date.atTime(LocalTime.MAX).atZone(zoneId).toInstant()
            val startTimestamp = Timestamp(startInstant.epochSecond, startInstant.nano)
            val endTimestamp = Timestamp(endInstant.epochSecond, endInstant.nano)

            val snapshot = appointmentsCollection
                .whereEqualTo("ownerId", ownerId)
                .whereGreaterThanOrEqualTo("appointmentDateTime", startTimestamp)
                .whereLessThanOrEqualTo("appointmentDateTime", endTimestamp)
                .get()
                .await()

            val appointments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Appointment::class.java)?.copy(id = doc.id)
            }
            Result.Success(appointments)
        } catch (e: Exception) {
            Result.Error(e, "Randevular alınamadı.")
        }
    }

    override fun getMyBookingsStream(userId: String): Flow<Result<List<Appointment>>> = callbackFlow {
        val listenerRegistration = appointmentsCollection
            .whereEqualTo("userId", userId)
            .orderBy("appointmentDateTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error, "Randevularım alınamadı."))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        val bookings = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Appointment::class.java)?.copy(id = doc.id)
                        }
                        trySend(Result.Success(bookings))
                    } catch (e: Exception) {
                        trySend(Result.Error(e, "Randevu verileri işlenemedi."))
                    }
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun deleteAppointment(appointmentId: String): Result<Unit> {
        return try {
            appointmentsCollection.document(appointmentId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting appointment with ID: $appointmentId")
            Result.Error(e, "Randevu kaydı silinemedi.")
        }
    }

    override suspend fun createAppointmentAndSlot(
        appointment: Appointment,
        slot: BookedSlot
    ): Result<Unit> {
        return try {
            val appointmentRef = appointmentsCollection.document()
            val appointmentId = appointmentRef.id

            val slotRef = slotsCollection.document()

            firestore.runBatch { batch ->
                batch.set(appointmentRef, appointment.copy(id = appointmentId))
                batch.set(slotRef, slot.copy(appointmentId = appointmentId, ownerId = appointment.ownerId))
            }.await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create appointment and slot in a batch write.")
            Result.Error(e, "Randevu oluşturulurken bir hata oluştu.")
        }
    }

}