package com.stellarforge.composebooking.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class SlotRemoteDataSource @Inject constructor(
    private val  firestore: FirebaseFirestore
) {

    private val slotsCollection: CollectionReference = firestore.collection(FirebaseConstants.BOOKED_SLOTS_COLLECTION)

    /**
     * Yeni bir BookedSlot belgesini Firestore'a ekler.
     * Otomatik ID kullanır.
     */
    suspend fun addSlot(slot: BookedSlot): Result<Unit> {
        return try {
            slotsCollection.add(slot).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Belirli bir tarihteki dolu slotları Firestore'dan getirir.
     * Sadece başlangıç ve bitiş zamanlarını içeren slotları getirir.
     * Başlangıç zamanına göre sıralar.
     */
    suspend fun getSlotsForDate(ownerId: String, date: LocalDate): Result<List<BookedSlot>> {
        return try {
            val zoneId = ZoneId.systemDefault()
            val startOfDay = date.atStartOfDay(zoneId).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant() // Bitiş: Sonraki günün başlangıcı

            val startTimestamp = Timestamp(startOfDay.epochSecond, startOfDay.nano)
            val endTimestamp = Timestamp(endOfDay.epochSecond, endOfDay.nano)

            // Sorgu: Başlangıç zamanı (startTime) belirtilen gün içinde olanlar
            // VEYA Bitiş zamanı (endTime) belirtilen gün içinde olanlar?
            // Daha basit: Başlangıç zamanı o gün olanları alalım.
            // (Gün aşan randevular varsa bu sorgu yetmeyebilir, ama şimdilik yeterli)
            val snapshot = slotsCollection
                .whereEqualTo("ownerId", ownerId) // <-- YENİ FİLTRE
                .whereGreaterThanOrEqualTo("startTime", startTimestamp)
                .whereLessThan("startTime", endTimestamp)
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

    /**
     * Belirli bir appointmentId'ye sahip BookedSlot belgesini siler.
     * (Randevu iptali/silinmesi durumunda kullanılabilir)
     */
    suspend fun deleteSlotByAppointmentId(appointmentId: String): Result<Unit> {
        return try {
            val querySnapshot = slotsCollection.whereEqualTo("appointmentId", appointmentId).get().await()
            if (!querySnapshot.isEmpty) {
                for (document in querySnapshot.documents) {
                    document.reference.delete().await()
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Slot silinemedi.")
        }
    }

}