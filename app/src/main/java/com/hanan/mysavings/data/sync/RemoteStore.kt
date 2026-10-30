package com.hanan.mysavings.data.sync

/**
 * One row as stored in the cloud. [fields] holds the table's data columns plus the parent
 * reference (e.g. "goalUuid"); a deleted row keeps only its uuid, time and [deleted] flag.
 */
data class RemoteDoc(
    val uuid: String,
    /** When the row last changed on the phone that changed it; decides conflicts. */
    val updatedAt: Long,
    val deleted: Boolean,
    val fields: Map<String, Any?>,
    /**
     * When the cloud received it, by the server's clock (null for documents not uploaded yet).
     * Used only to ask "what is new since last time": a phone with a wrong clock cannot make
     * other phones miss its changes, because this time is never taken from a phone.
     */
    val serverTime: Long? = null,
)

/**
 * The cloud copy, per account. An interface so the sync logic can be tested against an
 * in-memory fake instead of the real Firestore.
 */
interface RemoteStore {
    suspend fun push(uid: String, collection: String, docs: List<RemoteDoc>)

    /** Documents the server received at or after [sinceServerTime], oldest first. */
    suspend fun pull(uid: String, collection: String, sinceServerTime: Long): List<RemoteDoc>

    /** Replaces the account's profile photo in the cloud. */
    suspend fun uploadPhoto(uid: String, version: Long, bytes: ByteArray)

    /** The account's profile photo in the cloud, or `null` if there is none. */
    suspend fun downloadPhoto(uid: String): RemotePhoto?

    suspend fun deletePhoto(uid: String)

    /** Removes everything stored for the account (when the account is deleted). */
    suspend fun deleteAll(uid: String)
}

class RemotePhoto(val version: Long, val bytes: ByteArray)
