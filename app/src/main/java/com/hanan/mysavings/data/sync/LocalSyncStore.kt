package com.hanan.mysavings.data.sync

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hanan.mysavings.data.local.MySavingsDatabase
import com.hanan.mysavings.data.local.entity.SyncTombstoneEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Room side of sync, for every table in [SyncTables] at once.
 *
 * It works on plain SQL driven by each [SyncTable] instead of seven typed DAOs: the sync
 * logic is the same for every table, and one generic implementation keeps it in one place.
 * Writes run inside Room's transactions, so screens observing these tables still refresh.
 * Callers run it off the main thread (the sync worker does).
 */
@Singleton
class LocalSyncStore @Inject constructor(
    private val database: MySavingsDatabase,
) {
    private val db: SupportSQLiteDatabase get() = database.openHelper.writableDatabase

    /** Rows changed on this phone and not uploaded yet, ready to send. */
    fun dirtyRows(table: SyncTable): List<RemoteDoc> {
        val parent = table.parent
        val sql = if (parent == null) {
            "SELECT * FROM `${table.name}` WHERE dirty = 1"
        } else {
            // LEFT JOIN: the overall budget has no category, its categoryKey is then null.
            "SELECT t.*, p.`${parent.parentKeyColumn}` AS `${parent.field}` FROM `${table.name}` t " +
                "LEFT JOIN `${parent.parentTable}` p ON p.id = t.`${parent.column}` WHERE t.dirty = 1"
        }
        return db.query(sql).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val fields = (table.columns + listOfNotNull(parent?.field)).associateWith { cursor.value(it) }
                    add(
                        RemoteDoc(
                            uuid = cursor.getString(cursor.getColumnIndexOrThrow("uuid")),
                            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt")),
                            deleted = false,
                            fields = fields,
                        ),
                    )
                }
            }
        }
    }

    /**
     * Marks uploaded rows as clean, unless they changed again while uploading: the updatedAt
     * check leaves such a row dirty, so the newer edit goes up next time.
     */
    fun markClean(table: SyncTable, uploaded: List<RemoteDoc>) = database.runInTransaction {
        uploaded.forEach {
            db.execSQL("UPDATE `${table.name}` SET dirty = 0 WHERE uuid = ? AND updatedAt = ?", arrayOf<Any>(it.uuid, it.updatedAt))
        }
    }

    fun tombstones(): List<SyncTombstoneEntity> = db.query("SELECT collection, uuid, deletedAt FROM sync_tombstones").use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(SyncTombstoneEntity(cursor.getString(0), cursor.getString(1), cursor.getLong(2)))
        }
    }

    fun removeTombstones(uploaded: List<SyncTombstoneEntity>) = database.runInTransaction {
        uploaded.forEach {
            // Same check as markClean: a newer deletion time means it was deleted again meanwhile.
            db.execSQL(
                "DELETE FROM sync_tombstones WHERE collection = ? AND uuid = ? AND deletedAt = ?",
                arrayOf<Any>(it.collection, it.uuid, it.deletedAt),
            )
        }
    }

    /**
     * Applies downloaded documents to [table], newest change wins (see [SyncRules]).
     * Applied rows are stored as clean: they came from the cloud, there is nothing to upload.
     */
    fun applyRemote(table: SyncTable, docs: List<RemoteDoc>) = database.runInTransaction {
        docs.forEach { doc ->
            val local = updatedAtOf(table, doc.uuid)
            val tombstone = tombstoneOf(table, doc.uuid)
            when (SyncRules.decide(local, tombstone, doc)) {
                RemoteAction.SKIP -> Unit
                // Deletes children through CASCADE too: the phone that deleted them sent their tombstones as well.
                RemoteAction.DELETE_LOCAL -> db.execSQL("DELETE FROM `${table.name}` WHERE uuid = ?", arrayOf(doc.uuid))
                RemoteAction.APPLY -> {
                    // A newer edit elsewhere wins over a deletion here: the row comes back.
                    if (tombstone != null) {
                        db.execSQL("DELETE FROM sync_tombstones WHERE collection = ? AND uuid = ?", arrayOf(table.name, doc.uuid))
                    }
                    upsert(table, doc, exists = local != null)
                }
            }
        }
    }

    /** Every synced row back to "not uploaded", e.g. when a different account signs in. */
    fun markAllDirty() = database.runInTransaction {
        SyncTables.ALL.forEach { db.execSQL("UPDATE `${it.name}` SET dirty = 1") }
    }

    private fun upsert(table: SyncTable, doc: RemoteDoc, exists: Boolean) {
        val values = ContentValues().apply {
            table.columns.forEach { putAny(it, doc.fields[it]) }
            put("uuid", doc.uuid)
            put("updatedAt", doc.updatedAt)
            put("dirty", 0)
        }
        table.parent?.let { parent ->
            val key = doc.fields[parent.field]
            val parentId = key?.let { parentIdOf(parent, it.toString()) }
            // Parent unknown here (deleted on this phone, or a category this version lacks): skip
            // the row rather than attach it to the wrong parent.
            if (key != null && parentId == null) return
            values.putAny(parent.column, parentId)
        }
        if (exists) {
            db.update(table.name, SQLiteDatabase.CONFLICT_ABORT, values, "uuid = ?", arrayOf(doc.uuid))
        } else {
            table.fixedId?.let { values.put("id", it) }
            // ABORT, never REPLACE: replacing would silently delete a clashing row (and its children).
            db.insert(table.name, SQLiteDatabase.CONFLICT_ABORT, values)
        }
    }

    private fun updatedAtOf(table: SyncTable, uuid: String): Long? =
        db.query("SELECT updatedAt FROM `${table.name}` WHERE uuid = ?", arrayOf(uuid)).use { if (it.moveToFirst()) it.getLong(0) else null }

    private fun tombstoneOf(table: SyncTable, uuid: String): Long? =
        db.query("SELECT deletedAt FROM sync_tombstones WHERE collection = ? AND uuid = ?", arrayOf(table.name, uuid))
            .use { if (it.moveToFirst()) it.getLong(0) else null }

    private fun parentIdOf(parent: ParentRef, key: String): Long? =
        db.query("SELECT id FROM `${parent.parentTable}` WHERE `${parent.parentKeyColumn}` = ?", arrayOf(key))
            .use { if (it.moveToFirst()) it.getLong(0) else null }

    private fun Cursor.value(column: String): Any? {
        val index = getColumnIndexOrThrow(column)
        return when (getType(index)) {
            Cursor.FIELD_TYPE_NULL -> null
            Cursor.FIELD_TYPE_INTEGER -> getLong(index)
            else -> getString(index)
        }
    }

    /** Firestore hands numbers back as Long and text as String; null stays null. */
    private fun ContentValues.putAny(key: String, value: Any?) = when (value) {
        null -> putNull(key)
        is Long -> put(key, value)
        is Int -> put(key, value.toLong())
        is Number -> put(key, value.toLong())
        else -> put(key, value.toString())
    }
}
