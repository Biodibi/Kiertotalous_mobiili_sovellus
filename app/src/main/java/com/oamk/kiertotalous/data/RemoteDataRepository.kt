package com.oamk.kiertotalous.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.oamk.kiertotalous.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class RemoteDataRepository(private val firebaseAuth: FirebaseAuth,
                       private val firebaseMessaging: FirebaseMessaging,
                       private val firebaseStorage: FirebaseStorage,
                       private val fireDatabase: FirebaseFirestore,
                       private val firebaseInstallations: FirebaseInstallations) {

    private val userAccountsCollection = fireDatabase.collection("userAccounts")
    private val sitesCollection = fireDatabase.collection("sites")
    private val deliveriesCollection = fireDatabase.collection("deliveries")
    private val summaryCollection = fireDatabase.collection("summary")
    private val imageStorage = firebaseStorage.reference.child("images")

    private var deliveriesSubscription: ListenerRegistration? = null

    suspend fun loginWithEmailAndPassword(email: String, password: String): Flow<FirebaseResult<UserAccount>> = flow<FirebaseResult<UserAccount>> {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        result.user?.let { user ->
            val queryResult = userAccountsCollection.whereEqualTo("userId", user.uid).get().await()
            queryResult.firstOrNull()?.toObject(UserAccount::class.java)?.let { userAccount ->
                emit(FirebaseResult.Success(userAccount))
            } ?: run {
                throw Throwable("User account does not exist")
            }
        } ?: run {
            throw Throwable("User does not exist")
        }
    }.catch { error ->
        emit(FirebaseResult.Error(error))
    }.flowOn(Dispatchers.IO)

    suspend fun upload(fileInfo: FileInfo): Flow<FirebaseResult<StorageReference>> = flow<FirebaseResult<StorageReference>> {
        val result = imageStorage.child(fileInfo.fileName).putFile(fileInfo.contentUri, fileInfo.metadata).await()
        emit(FirebaseResult.Success(result.storage))
    }.catch { error ->
        emit(FirebaseResult.Error(error))
    }.flowOn(Dispatchers.IO)

    suspend fun subscribeToTopic(topic: String): Flow<FirebaseResult<Any>> = flow<FirebaseResult<Any>> {
        firebaseMessaging.subscribeToTopic(topic).await()
        emit(FirebaseResult.Success(Any()))
    }.catch { error ->
        emit(FirebaseResult.Error(error))
    }.flowOn(Dispatchers.IO)

    suspend fun submitDeliveryForm(delivery: Delivery): Flow<FirebaseResult<DocumentReference>> = flow<FirebaseResult<DocumentReference>> {
        val result = deliveriesCollection.add(delivery).await()
        emit(FirebaseResult.Success(result))
    }.catch { error ->
        emit(FirebaseResult.Error(error))
    }.flowOn(Dispatchers.IO)

    suspend fun saveSummary(summary: Summary): Flow<FirebaseResult<DocumentReference>> = flow<FirebaseResult<DocumentReference>> {
        val fetchResult = deliveriesCollection.whereIn("uid", summary.deliveries).get().await()
        fetchResult.forEach { snapshot ->
            snapshot.reference.update("status", DeliveryStatus.COMPLETED.status)
            snapshot.reference.update("modified", Instant.now().toString())
        }

        val result = summaryCollection.add(summary).await()
        emit(FirebaseResult.Success(result))
    }.catch { error ->
        emit(FirebaseResult.Error(error))
    }.flowOn(Dispatchers.IO)

    suspend fun fetchSites(): Flow<FirebaseResult<List<Site>>> = flow<FirebaseResult<List<Site>>> {
        val result = sitesCollection.get().await()
        emit(FirebaseResult.Success(result.toObjects(Site::class.java)))
    }.catch { error ->
        emit(FirebaseResult.Error(error))
    }.flowOn(Dispatchers.IO)

    suspend fun fetchSite(siteId: String): Flow<FirebaseResult<Site>> = flow<FirebaseResult<Site>> {
        val result = sitesCollection.whereEqualTo("uid", siteId).get().await()
        result.firstOrNull()?.toObject(Site::class.java)?.let { site ->
            emit(FirebaseResult.Success(site))
        } ?: run {
            throw Throwable("Site does not exist")
        }
    }.catch { error ->
        emit(FirebaseResult.Error(error))
    }.flowOn(Dispatchers.IO)

    fun subscribeToDeliveries() = callbackFlow<FirebaseResult<List<Delivery>>> {
        val offset = ZonedDateTime.now().offset.totalSeconds.toLong()
        val startOfDayInUtc = LocalDate.now().atStartOfDay().minusSeconds(offset).toInstant(ZoneOffset.UTC).toString()
        val query = deliveriesCollection.whereGreaterThanOrEqualTo("created", startOfDayInUtc)

        deliveriesSubscription?.remove()
        deliveriesSubscription = query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            when {
                snapshot != null -> {
                    if (!snapshot.metadata.isFromCache) {
                        snapshot.toObjects(Delivery::class.java).let { deliveries ->
                            val openDeliveries = deliveries.filter { delivery -> delivery.status == DeliveryStatus.OPEN.status }
                            trySend(FirebaseResult.Success(openDeliveries))
                        }
                    }
                }
                error != null -> {
                    throw error
                }
                else -> {
                    return@addSnapshotListener
                }
            }
        }
        awaitClose {
            deliveriesSubscription?.remove()
        }
    }.catch { error ->
        emit(FirebaseResult.Error(error))
    }.flowOn(Dispatchers.IO)

    fun getStorageReference(fileName: String): StorageReference {
        return imageStorage.child(fileName)
    }

    fun unsubscribe() {
        deliveriesSubscription?.remove()
    }

    suspend fun logout() {
        unsubscribe()

        firebaseMessaging.isAutoInitEnabled = false
        firebaseInstallations.delete().await()
        firebaseMessaging.deleteToken().await()
        firebaseAuth.signOut()
    }
}