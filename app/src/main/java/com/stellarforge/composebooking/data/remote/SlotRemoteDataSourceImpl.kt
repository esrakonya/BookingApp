package com.stellarforge.composebooking.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

class SlotRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SlotRemoteDataSource {

    private val slotsCollection: CollectionReference = firestore.collection(FirebaseConstants.BOOKED_SLOTS_COLLECTION)

    override suspend fun addSlot(slot: BookedSlot): Result<Unit> {
        return try {
            val docRef = slotsCollection.document()
            docRef.set(slot.copy(id = docRef.id)).await()
            Result.Success(Unit)
        } catch (e: Exception) { Result.Error(e) }
    }

    override suspend fun getSlotsForDate(
        ownerId: String,
        date: LocalDate
    ): Result<List<BookedSlot>> {
        return try {
            val zoneId = ZoneId.systemDefault()
            val startOfDay = date.atStartOfDay(zoneId).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant()

            // DÜZELTME: Instant'ı Date'e çevirerek Timestamp oluşturuyoruz.
            val startTimestamp = Timestamp(Date.from(startOfDay))
            val endTimestamp = Timestamp(Date.from(endOfDay))

            val snapshot = slotsCollection
                .whereEqualTo("ownerId", ownerId)
                .whereGreaterThanOrEqualTo("startTime", startTimestamp)
                .whereLessThan("startTime", endTimestamp) // `whereLessThanOrEqualTo` yerine `whereLessThan` kullanmak daha güvenlidir.
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()

            val slots = snapshot.documents.mapNotNull { doc ->
                doc.toObject(BookedSlot::class.java)?.copy(id = doc.id)
            }
            Result.Success(slots)
        } catch (e: Exception) {
            Result.Error(e, "Dolu saatler alınamadı.")
        }
    }

    override suspend fun deleteSlotByAppointmentId(appointmentId: String): Result<Unit> {
        return try {
            val querySnapshot = slotsCollection.whereEqualTo("appointmentId", appointmentId).get().await()
            querySnapshot.documents.forEach { it.reference.delete().await() }
            Result.Success(Unit)
        } catch (e: Exception) { Result.Error(e, "Slot silinemedi.") }
    }

}