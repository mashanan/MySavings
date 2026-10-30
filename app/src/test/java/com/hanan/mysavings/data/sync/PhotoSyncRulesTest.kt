package com.hanan.mysavings.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoSyncRulesTest {

    @Test
    fun `a photo set on this phone goes up once`() {
        assertEquals(PhotoAction.UPLOAD, PhotoSyncRules.decide(rowVersion = 5, fileVersion = 5, cloudVersion = null))
        assertEquals(PhotoAction.UPLOAD, PhotoSyncRules.decide(rowVersion = 5, fileVersion = 5, cloudVersion = 3))
        assertEquals(PhotoAction.NOTHING, PhotoSyncRules.decide(rowVersion = 5, fileVersion = 5, cloudVersion = 5))
    }

    @Test
    fun `a photo set on another phone comes down`() {
        assertEquals(PhotoAction.DOWNLOAD, PhotoSyncRules.decide(rowVersion = 7, fileVersion = 5, cloudVersion = 5))
        assertEquals(PhotoAction.DOWNLOAD, PhotoSyncRules.decide(rowVersion = 7, fileVersion = null, cloudVersion = null))
    }

    @Test
    fun `a removed photo is removed everywhere, and then nothing is left to do`() {
        assertEquals(PhotoAction.REMOVE, PhotoSyncRules.decide(rowVersion = null, fileVersion = 5, cloudVersion = 5))
        assertEquals(PhotoAction.REMOVE, PhotoSyncRules.decide(rowVersion = null, fileVersion = null, cloudVersion = 5))
        assertEquals(PhotoAction.NOTHING, PhotoSyncRules.decide(rowVersion = null, fileVersion = null, cloudVersion = null))
    }
}
