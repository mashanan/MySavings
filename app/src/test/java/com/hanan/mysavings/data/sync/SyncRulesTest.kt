package com.hanan.mysavings.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncRulesTest {

    private fun remote(updatedAt: Long, deleted: Boolean = false) =
        RemoteDoc(uuid = "u", updatedAt = updatedAt, deleted = deleted, fields = emptyMap())

    @Test
    fun `a row this phone does not have is inserted`() {
        assertEquals(RemoteAction.APPLY, SyncRules.decide(localUpdatedAt = null, tombstoneAt = null, remote = remote(100)))
    }

    @Test
    fun `the newer change wins, in both directions`() {
        assertEquals(RemoteAction.APPLY, SyncRules.decide(100, null, remote(200)))
        assertEquals(RemoteAction.SKIP, SyncRules.decide(300, null, remote(200)))
    }

    @Test
    fun `the same version coming back from the cloud changes nothing`() {
        assertEquals(RemoteAction.SKIP, SyncRules.decide(200, null, remote(200)))
    }

    @Test
    fun `a newer deletion elsewhere removes the row here, an older one does not`() {
        assertEquals(RemoteAction.DELETE_LOCAL, SyncRules.decide(100, null, remote(200, deleted = true)))
        assertEquals(RemoteAction.SKIP, SyncRules.decide(300, null, remote(200, deleted = true)))
    }

    @Test
    fun `a deletion of a row this phone never had is ignored`() {
        assertEquals(RemoteAction.SKIP, SyncRules.decide(null, null, remote(200, deleted = true)))
    }

    @Test
    fun `a deletion here beats an older edit elsewhere, a newer edit brings the row back`() {
        assertEquals(RemoteAction.SKIP, SyncRules.decide(localUpdatedAt = null, tombstoneAt = 300, remote = remote(200)))
        assertEquals(RemoteAction.APPLY, SyncRules.decide(localUpdatedAt = null, tombstoneAt = 300, remote = remote(400)))
    }
}
