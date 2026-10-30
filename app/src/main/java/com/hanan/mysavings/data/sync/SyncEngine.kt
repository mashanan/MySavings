package com.hanan.mysavings.data.sync

import com.hanan.mysavings.data.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * One full sync of all tables between this phone and the account's cloud copy.
 *
 * Download first, then upload. Downloading first lets every incoming change be compared with
 * the local row before anything is sent, so an older local edit never overwrites a newer one
 * made on another phone (the newest change wins, see [SyncRules]).
 */
class SyncEngine @Inject constructor(
    private val local: LocalSyncStore,
    private val remote: RemoteStore,
    private val state: SyncStateStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun sync(uid: String) = withContext(ioDispatcher) {
        if (state.lastUid != uid) {
            // First sync with this account on this phone (or a different account than before):
            // everything here is new to it, so upload all of it, and download all of its data.
            local.markAllDirty()
            state.switchTo(uid)
        }

        // Parents before children, so a downloaded deposit always finds its goal.
        SyncTables.ALL.forEach { table ->
            val docs = remote.pull(uid, table.name, state.cursor(table.name))
            local.applyRemote(table, docs)
            docs.mapNotNull { it.serverTime }.maxOrNull()?.let { state.setCursor(table.name, it) }
        }

        SyncTables.ALL.forEach { table ->
            val rows = local.dirtyRows(table)
            if (rows.isNotEmpty()) {
                remote.push(uid, table.name, rows)
                local.markClean(table, rows)
            }
        }

        val tombstones = local.tombstones()
        tombstones.groupBy { it.collection }.forEach { (collection, deleted) ->
            remote.push(uid, collection, deleted.map { RemoteDoc(it.uuid, it.deletedAt, deleted = true, fields = emptyMap()) })
        }
        local.removeTombstones(tombstones)
    }
}
