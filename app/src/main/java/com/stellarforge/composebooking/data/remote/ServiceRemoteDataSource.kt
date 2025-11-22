package com.stellarforge.composebooking.data.remote

import com.stellarforge.composebooking.data.model.Service
import com.google.firebase.firestore.CollectionReference
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.stellarforge.composebooking.utils.DocumentNotFoundException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Servis verileri için ham veri kaynağı işlemlerini tanımlayan arayüz.
 */
interface ServiceRemoteDataSource {
    fun getOwnerServicesStream(ownerId: String): Flow<Result<List<Service>>>

    suspend fun addService(service: Service): Result<Unit>

    suspend fun updateService(service: Service): Result<Unit>

    suspend fun deleteService(serviceId: String): Result<Unit>

    suspend fun getServiceDetails(serviceId: String): Result<Service>
}

/*
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
     * GÜNCELLENDİ: Artık belirli bir işletme sahibine ait servisleri dinler.
     *
     * @param ownerId Servisleri listelenecek işletme sahibinin UID'si.
     * @return Servis listesini veya hatayı içeren bir Flow<Result<List<Service>>>.
     */
    fun getOwnerServicesStream(ownerId: String): Flow<Result<List<Service>>> = callbackFlow {
        // Sorgu, ownerId'ye göre filtreleme ve isme göre sıralama yapacak şekilde güncellendi.
        val listenerRegistration = serviceCollection
            .whereEqualTo("ownerId", ownerId)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        // Önceki kodundaki gibi ID'leri manuel olarak atıyoruz.
                        val services = snapshot.documents.mapNotNull { doc ->
                            doc.toObject<Service>()?.copy(id = doc.id)
                        }
                        trySend(Result.Success(services))
                    } catch (e: Exception) {
                        trySend(Result.Error(e))
                    }
                }
            }
        // Listener'ı coroutine iptal edildiğinde kaldır.
        awaitClose { listenerRegistration.remove() }
    }

    /**
     * YENİ FONKSİYON: Yeni bir servis dökümanını Firestore'a ekler.
     *
     * @param service Eklenecek olan Service nesnesi. 'ownerId' alanı dolu olmalıdır.
     * @return Başarı durumunda Unit veya hata durumunda Exception içeren bir Result.
     */
    suspend fun addService(service: Service): Result<Unit> {
        return try {
            val documentReference = serviceCollection.document()
            // ID'yi de içeren bir nesneyle dökümanı set ediyoruz.
            // `createdAt` ve `updatedAt` @ServerTimestamp ile otomatik dolacak.
            documentReference.set(service.copy(id = documentReference.id)).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Servis eklenemedi.")
        }
    }

    /**
     * YENİ FONKSİYON: Mevcut bir servisi Firestore'da günceller.
     *
     * @param service Güncellenmiş verileri içeren Service nesnesi. 'id' alanı dolu olmalıdır.
     * @return Başarı durumunda Unit veya hata durumunda Exception içeren bir Result.
     */
    suspend fun updateService(service: Service): Result<Unit> {
        return try {
            // `updatedAt` @ServerTimestamp ile otomatik güncellenecek.
            serviceCollection.document(service.id).set(service).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Servis güncellenemedi.")
        }
    }

    /**
     * YENİ FONKSİYON: Firestore'dan belirli bir servis dökümanını siler.
     *
     * @param serviceId Silinecek olan servisin döküman ID'si.
     * @return Başarı durumunda Unit veya hata durumunda Exception içeren bir Result.
     */
    suspend fun deleteService(serviceId: String): Result<Unit> {
        return try {
            serviceCollection.document(serviceId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Servis silinemedi.")
        }
    }

    /**
     * Mevcut fonksiyonun adı, amacını daha iyi yansıtacak şekilde 'getServiceById' olarak değiştirilebilir,
     * ama şimdilik aynı bırakıyorum.
     * @param serviceId Getirilecek servisin Firestore belge ID'si.
     * @return Servis detayını veya hatayı içeren bir Result<Service>.
     */
    suspend fun getServiceDetails(serviceId: String): Result<Service> {
        return try {
            val documentSnapshot = serviceCollection.document(serviceId).get().await()
            val service = documentSnapshot.toObject<Service>()?.copy(id = documentSnapshot.id)
            if (service != null) {
                Result.Success(service)
            } else {
                Result.Error(DocumentNotFoundException("Service with ID $serviceId not found."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

 */