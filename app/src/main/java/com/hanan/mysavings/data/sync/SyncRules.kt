package com.hanan.mysavings.data.sync

/** What to do with one downloaded document. */
enum class RemoteAction {
    /** Insert or overwrite the local row. */
    APPLY,

    /** The row was deleted elsewhere, after the last change here. */
    DELETE_LOCAL,

    /** This phone already has the same or a newer version (or deletion). */
    SKIP,
}

/**
 * The conflict rule, in one place: the newest change wins, a deletion counts as a change.
 * Kept free of Room and Firestore so it can be unit tested on its own.
 */
object SyncRules {
    /**
     * @param localUpdatedAt the local row's updatedAt, `null` if there is no such row
     * @param tombstoneAt when the row was deleted here and that is not uploaded yet, else `null`
     */
    fun decide(localUpdatedAt: Long?, tombstoneAt: Long?, remote: RemoteDoc): RemoteAction {
        // The newest thing this phone knows about the row: its last edit or its deletion.
        val known = maxOf(localUpdatedAt ?: Long.MIN_VALUE, tombstoneAt ?: Long.MIN_VALUE)
        return when {
            // Equal times: the same change coming back from the cloud (often our own upload).
            remote.updatedAt <= known -> RemoteAction.SKIP
            remote.deleted -> if (localUpdatedAt != null) RemoteAction.DELETE_LOCAL else RemoteAction.SKIP
            else -> RemoteAction.APPLY
        }
    }
}
