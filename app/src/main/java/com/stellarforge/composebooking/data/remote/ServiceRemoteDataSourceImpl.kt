package com.stellarforge.composebooking.data.remote

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.utils.DocumentNotFoundException
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Concrete implementation of [ServiceRemoteDataSource] utilizing Cloud Firestore.
 *
 * This class manages the CRUD operations for the 'services' collection.
 * It leverages Kotlin [Flow] and Firestore [SnapshotListener] to provide
 * reactive, real-time data updates to the UI without manual refreshing.
 *
 * @param firestore The Firestore instance injected via Hilt.
 */
class ServiceRemoteDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ServiceRemoteDataSource {
    private val serviceCollection: CollectionReference = firestore.collection(FirebaseConstants.SERVICES_COLLECTION)

    /**
     * Listens for real-time updates of services belonging to a specific owner.
     * Used in the "Manage Services" screen.
     */
    override fun getOwnerServicesStream(ownerId: String): Flow<Result<List<Service>>> = callbackFlow {
        val listener = serviceCollection
            .whereEqualTo("ownerId", ownerId)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        val services = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Service::class.java)?.copy(id = doc.id)
                        }
                        trySend(Result.Success(services))
                    } catch (e: Exception) {
                        trySend(Result.Error(e))
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Listens for real-time updates of ACTIVE services.
     * Used in the "Customer Home" screen (Storefront).
     */
    override fun getCustomerServicesStream(): Flow<Result<List<Service>>> = callbackFlow {
        val listener = serviceCollection
            .whereEqualTo("isActive", true) // Only show active services to customers
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        val services = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Service::class.java)?.copy(id = doc.id)
                        }
                        trySend(Result.Success(services))
                    } catch (e: Exception) {
                        trySend(Result.Error(e))
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addService(service: Service): Result<Unit> {
        return try {
            val docRef = serviceCollection.document()
            docRef.set(service.copy(id = docRef.id)).await()
            Result.Success(Unit)
        } catch (e: Exception) { Result.Error(e, "Failed to add service.") }
    }

    override suspend fun updateService(service: Service): Result<Unit> {
        return try {
            serviceCollection.document(service.id).set(service).await()
            Result.Success(Unit)
        } catch (e: Exception) { Result.Error(e, "Failed to update service.") }
    }

    override suspend fun deleteService(serviceId: String): Result<Unit> {
        return try {
            serviceCollection.document(serviceId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) { Result.Error(e, "Failed to delete service.") }
    }

    override suspend fun getServiceDetails(serviceId: String): Result<Service> {
        return try {
            val document = serviceCollection.document(serviceId).get().await()
            val service = document.toObject(Service::class.java)?.copy(id = document.id)
            if (service != null) {
                Result.Success(service)
            } else {
                Result.Error(DocumentNotFoundException("Service with ID $serviceId not found."))
            }
        } catch (e: Exception) { Result.Error(e) }
    }

}