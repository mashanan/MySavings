package com.hanan.mysavings.data.sync

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

/**
 * The cloud copy in Firestore: `users/{uid}/{collection}/{uuid}`. The security rules
 * (firestore.rules) let an account read and write only below its own uid.
 */
class FirestoreRemoteStore @Inject constructor(
    private val firestore: FirebaseFirestore,
) : RemoteStore {

    private fun collection(uid: String, name: String) = firestore.collection("users").document(uid).collection(name)

    override suspend fun push(uid: String, collection: String, docs: List<RemoteDoc>) {
        // Firestore batches hold at most 500 writes.
        docs.chunked(BATCH_SIZE).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { doc ->
                // set() replaces the whole document, so a deletion also drops the old data.
                batch.set(
                    collection(uid, collection).document(doc.uuid),
                    doc.fields + mapOf(
                        FIELD_UPDATED_AT to doc.updatedAt,
                        FIELD_DELETED to doc.deleted,
                        FIELD_SERVER_TIME to FieldValue.serverTimestamp(),
                    ),
                )
            }
            batch.commit().await()
        }
    }

    override suspend fun pull(uid: String, collection: String, sinceServerTime: Long): List<RemoteDoc> {
        // ">=": documents stamped in the same millisecond as the last one seen are read again
        // rather than missed; applying a known version twice changes nothing.
        val snapshot = collection(uid, collection)
            .whereGreaterThanOrEqualTo(FIELD_SERVER_TIME, Timestamp(Date(sinceServerTime)))
            .orderBy(FIELD_SERVER_TIME)
            // Straight from the server: Room is the offline copy, Firestore's cache is not used.
            .get(Source.SERVER)
            .await()
        return snapshot.documents.map { document ->
            val data = document.data.orEmpty()
            RemoteDoc(
                uuid = document.id,
                updatedAt = (data[FIELD_UPDATED_AT] as? Number)?.toLong() ?: 0,
                deleted = data[FIELD_DELETED] as? Boolean ?: false,
                fields = data - META_FIELDS,
                serverTime = document.getTimestamp(FIELD_SERVER_TIME)?.toDate()?.time,
            )
        }
    }

    // One document, apart from the profile row: about 50 KB that only moves when the photo changes.
    private fun photoDocument(uid: String) =
        firestore.collection("users").document(uid).collection(PHOTO_COLLECTION).document(PHOTO_DOCUMENT)

    override suspend fun uploadPhoto(uid: String, version: Long, bytes: ByteArray) {
        photoDocument(uid).set(mapOf(FIELD_VERSION to version, FIELD_DATA to Blob.fromBytes(bytes))).await()
    }

    override suspend fun downloadPhoto(uid: String): RemotePhoto? {
        val document = photoDocument(uid).get(Source.SERVER).await()
        val version = (document.get(FIELD_VERSION) as? Number)?.toLong() ?: return null
        val data = document.getBlob(FIELD_DATA) ?: return null
        return RemotePhoto(version, data.toBytes())
    }

    override suspend fun deletePhoto(uid: String) {
        photoDocument(uid).delete().await()
    }

    /**
     * Deletes every document of the account, collection by collection. The users/{uid}
     * document itself never exists (only its sub-collections do), so nothing else is left.
     */
    override suspend fun deleteAll(uid: String) {
        (SyncTables.ALL.map { it.name } + PHOTO_COLLECTION).forEach { name ->
            val documents = collection(uid, name).get(Source.SERVER).await().documents
            documents.chunked(BATCH_SIZE).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }
    }

    private companion object {
        const val BATCH_SIZE = 400
        const val PHOTO_COLLECTION = "profile_photo"
        const val PHOTO_DOCUMENT = "current"
        const val FIELD_VERSION = "version"
        const val FIELD_DATA = "data"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_DELETED = "deleted"
        const val FIELD_SERVER_TIME = "serverUpdatedAt"
        val META_FIELDS = setOf(FIELD_UPDATED_AT, FIELD_DELETED, FIELD_SERVER_TIME)
    }
}
