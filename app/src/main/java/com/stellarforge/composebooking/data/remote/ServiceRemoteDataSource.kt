package com.stellarforge.composebooking.data.remote

import com.stellarforge.composebooking.data.model.Service
import com.google.firebase.firestore.CollectionReference
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Servis verileriyle ilgili Firebase Firestore işlemlerini yürüten veri kaynağı sınıfı.
 */
@Singleton
class ServiceRemoteDataSource @Inject constructor(
    private val firestore: Provider<FirebaseFirestore>
) {
    private val db: FirebaseFirestore get() = firestore.get()

    // 'services' koleksiyonuna referansı Constants dosyasından alarak oluştur
    private val serviceCollection: CollectionReference =
        db.collection(FirebaseConstants.SERVICES_COLLECTION)

    /**
     * Firestore'dan aktif servislerin listesini gerçek zamanlı olarak dinleyen bir Flow döndürür.
     * Veritabanındaki değişiklikler Flow aracılığıyla otomatik olarak yayılır.
     *
     * @return Aktif servis listesini veya hatayı içeren bir Flow<Result<List<Service>>>.
     */
    fun getServicesFlow(): Flow<Result<List<Service>>> = callbackFlow {
        val listenerRegistration = serviceCollection
            .whereEqualTo("isActive", true)
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val services = snapshot.toObjects(Service::class.java)

                        snapshot.documents.forEachIndexed { index, document ->
                            if (index < services.size) {
                                services[index].id = document.id
                            }
                        }
                        trySend(Result.success(services))
                    } catch (e: Exception) {
                        trySend(Result.failure(e))
                    }
                } else {
                    trySend(Result.success(emptyList()))
                }
            }
        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Belirtilen ID'ye sahip tek bir servisin detaylarını getirir.
     * @param serviceId Getirilecek servisin Firestore belge ID'si.
     * @return Servis detayını veya hatayı içeren bir Result<Service>.
     */
    suspend fun getServiceDetails(serviceId: String): Result<Service> {
        return try {
            val documentSnapshot = serviceCollection.document(serviceId).get().await()
            // Belgeyi Service nesnesine dönüştür, null kontrolü yap
            val service = documentSnapshot.toObject<Service>()?.apply {
                // Nesneye belge ID'sini ata
                id = documentSnapshot.id
            }

            if (service != null) {
                Result.success(service) // Başarılı olursa servisi döndür
            } else {
                // Belge bulunamadı veya dönüştürülemedi
                Result.failure(Exception("Service with ID $serviceId not found or could not be parsed."))
            }
        } catch (e: Exception) {
            // Firestore işlemi sırasında hata oluşursa
            Result.failure(e)
        }
    }
}