package com.stellarforge.composebooking.data.remote

import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Randevu verileriyle ilgili Firebase Firestore işlemlerini yürüten veri kaynağı sınıfı.
 */
@Singleton
class AppointmentRemoteDataSource @Inject constructor(
    private val firestore: Provider<FirebaseFirestore>
) {
    private val db: FirebaseFirestore get() = firestore.get()

    // 'appointments' koleksiyonuna referansı Constants dosyasından alarak oluştur
    private val appointmentsCollection: CollectionReference = db.collection(FirebaseConstants.APPOINTMENTS_COLLECTION)

    /**
     * Belirli bir tarihteki tüm randevuları Firestore'dan getiren suspend fonksiyonu.
     * Bu, uygun saat aralıklarını belirlemek için kullanılabilir.
     *
     * @param date Randevuların alınacağı tarih.
     * @return İlgili tarihteki randevu listesini veya hatayı içeren bir Result<List<Appointment>>.
     */
    suspend fun getAppointmentsForDate(ownerId: String, date: LocalDate): Result<List<Appointment>> {
        return try {
            val zoneId = ZoneId.systemDefault()
            val startInstant = date.atStartOfDay(zoneId).toInstant()
            val endInstant = date.atTime(LocalTime.MAX).atZone(zoneId).toInstant()

            val startTimestamp = Timestamp(startInstant.epochSecond, startInstant.nano)
            val endTimestamp = Timestamp(endInstant.epochSecond, endInstant.nano)

            // Firestore sorgusu: Belirtilen tarih aralığındaki randevuları getir
            val snapshot = appointmentsCollection
                .whereEqualTo("ownerId", ownerId)
                .whereGreaterThanOrEqualTo("appointmentDateTime", startTimestamp)
                .whereLessThanOrEqualTo("appointmentDateTime", endTimestamp)
                .get()
                .await()

            // GÜNCELLENDİ: ID ataması `copy()` ile yapılıyor.
            val appointments = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Appointment::class.java)?.copy(id = doc.id)
            }

            Result.Success(appointments)
        } catch (e: Exception) {
            Result.Error(e, "Randevular alınamadı.")
        }
    }

    /**
     * Yeni bir randevuyu Firestore'a kaydeder.
     * Firestore'un otomatik ID oluşturmasını kullanır.
     *
     * @param appointment Firestore'a kaydedilecek randevu nesnesi.
     * @return İşlemin başarılı olup olmadığını belirten bir Result<Unit>.
     */
    suspend fun createAppointment(appointment: Appointment): Result<Unit> {
        return try {
            // appointments koleksiyonuna yeni bir belge (randevu) ekle.
            // add() metodu otomatik olarak benzersiz bir ID oluşturur.
            appointmentsCollection.add(appointment).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createAppointmentWithId(ref: DocumentReference, appointment: Appointment): Result<Unit> {
        return try {
            ref.set(appointment).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}