package com.hanan.mysavings.data.local.entity

import androidx.room.ColumnInfo
import java.util.UUID

/**
 * Bookkeeping for sync (phase 11b), embedded in every synced table. Room flattens it, so a
 * table simply gets three more columns: `uuid`, `updatedAt`, `dirty`.
 *
 * The local `id` stays the primary key and foreign key target: it is fast and every query
 * already uses it. It means nothing on another phone, though, so rows are matched across
 * devices (and stored in the cloud) by [uuid].
 *
 * The column defaults match the `ALTER TABLE ... DEFAULT` of MIGRATION_5_6 exactly, so the
 * schema Room expects and the one the migration produces are identical.
 */
data class SyncColumns(
    /** Same on every device. Random for new rows; derived from content for budgets and the profile. */
    @ColumnInfo(defaultValue = "''") val uuid: String = UUID.randomUUID().toString(),
    /** Epoch millis of the last change on any device; the newest change wins a conflict. */
    @ColumnInfo(defaultValue = "0") val updatedAt: Long,
    /** Changed on this phone and not uploaded yet. */
    @ColumnInfo(defaultValue = "1") val dirty: Boolean = true,
) {
    /** The same row after a local edit at [now]: same uuid, newer time, waiting for upload. */
    fun touched(now: Long) = copy(updatedAt = now, dirty = true)
}
