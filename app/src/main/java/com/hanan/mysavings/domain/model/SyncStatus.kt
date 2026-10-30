package com.hanan.mysavings.domain.model

import java.time.Instant

/** What the account card says about sync. */
data class SyncStatus(
    /** When the last sync finished, `null` before the first one. */
    val lastSyncedAt: Instant?,
    val isSyncing: Boolean,
    /** The last attempt failed (e.g. no internet); it is retried automatically. */
    val lastAttemptFailed: Boolean,
)
