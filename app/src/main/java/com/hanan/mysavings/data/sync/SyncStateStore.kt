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
        putString(KEY_UID, uid)
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
    }
}

data class SyncResultRecord(
    /** Epoch millis of the last successful sync, `null` if there was none yet. */
    val lastSyncedAt: Long?,
    val lastAttemptFailed: Boolean,
)
