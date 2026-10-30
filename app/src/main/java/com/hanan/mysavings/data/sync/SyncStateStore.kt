package com.hanan.mysavings.data.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync bookkeeping that is not app data: which account the phone last synced with, how far
 * each collection has been downloaded, and the last result. A handful of values, so plain
 * SharedPreferences rather than another Room table and migration.
 */
@Singleton
class SyncStateStore(private val prefs: SharedPreferences) {

    // Tests pass their own SharedPreferences to play two phones in one process.
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    private val _lastResult = MutableStateFlow(readResult())

    /** For the account card: when the last sync finished, and whether the last attempt failed. */
    val lastResult: StateFlow<SyncResultRecord> = _lastResult.asStateFlow()

    val lastUid: String? get() = prefs.getString(KEY_UID, null)

    /** A different account starts from scratch: nothing of the old one was downloaded for it. */
    fun switchTo(uid: String) = prefs.edit {
        prefs.all.keys.filter { it.startsWith(KEY_CURSOR) }.forEach { remove(it) }
        remove(KEY_PHOTO)
        putString(KEY_UID, uid)
    }

    /** Version of the profile photo this phone knows is in the cloud; `null` if none. */
    var photoCloudVersion: Long?
        get() = prefs.getLong(KEY_PHOTO, -1).takeIf { it >= 0 }
        set(value) = prefs.edit { if (value == null) remove(KEY_PHOTO) else putLong(KEY_PHOTO, value) }

    /** Forgets everything, e.g. when the phone's data is erased on sign-out. */
    fun clear() {
        prefs.edit { clear() }
        _lastResult.value = readResult()
    }

    /** Server time of the newest document downloaded for [collection]; 0 before the first sync. */
    fun cursor(collection: String): Long = prefs.getLong(KEY_CURSOR + collection, 0)

    fun setCursor(collection: String, serverTime: Long) = prefs.edit { putLong(KEY_CURSOR + collection, serverTime) }

    fun recordSuccess(at: Long) {
        prefs.edit {
            putLong(KEY_LAST_SYNC, at)
            putBoolean(KEY_FAILED, false)
        }
        _lastResult.value = readResult()
    }

    fun recordFailure() {
        prefs.edit { putBoolean(KEY_FAILED, true) }
        _lastResult.value = readResult()
    }

    private fun readResult() = SyncResultRecord(
        lastSyncedAt = prefs.getLong(KEY_LAST_SYNC, 0).takeIf { it > 0 },
        lastAttemptFailed = prefs.getBoolean(KEY_FAILED, false),
    )

    private companion object {
        const val PREFS = "sync_state"
        const val KEY_UID = "uid"
        const val KEY_CURSOR = "cursor_"
        const val KEY_LAST_SYNC = "last_sync"
        const val KEY_FAILED = "last_failed"
        const val KEY_PHOTO = "photo_cloud_version"
    }
}

data class SyncResultRecord(
    /** Epoch millis of the last successful sync, `null` if there was none yet. */
    val lastSyncedAt: Long?,
    val lastAttemptFailed: Boolean,
)
