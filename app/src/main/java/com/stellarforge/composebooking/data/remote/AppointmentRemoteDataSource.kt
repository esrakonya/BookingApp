package com.stellarforge.composebooking.data.remote

import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
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
    suspend fun getAppointmentsForDate(date: LocalDate): Result<List<Appointment>> {
        return try {
            val zoneId = ZoneId.systemDefault()
            val startInstant = date.atStartOfDay(zoneId).toInstant()
            val endInstant = date.atTime(LocalTime.MAX).atZone(zoneId).toInstant()

            val startTimestamp = Timestamp(startInstant.epochSecond, startInstant.nano)
            val endTimestamp = Timestamp(endInstant.epochSecond, endInstant.nano)

            // Firestore sorgusu: Belirtilen tarih aralığındaki randevuları getir
            val snapshot = appointmentsCollection
                .whereGreaterThanOrEqualTo("appointmentDateTime", startTimestamp)
                .whereLessThanOrEqualTo("appointmentDateTime", endTimestamp)
                .get()
                .await()

            // Snapshot'taki belgeleri Appointment listesine dönüştür
            val appointments = snapshot.toObjects(Appointment::class.java)

            // Okunan her randevuya belge ID'sini (Firestore document ID) ekle
            snapshot.documents.forEachIndexed { index, document ->
                if (index < appointments.size) {
                    appointments[index].id = document.id
                }
            }

            Result.success(appointments)
        } catch (e: Exception) {
            Result.failure(e)
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAppointmentWithId(ref: DocumentReference, appointment: Appointment): Result<Unit> {
        return try {
            ref.set(appointment).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}