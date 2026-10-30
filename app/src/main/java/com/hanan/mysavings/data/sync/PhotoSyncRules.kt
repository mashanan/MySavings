package com.hanan.mysavings.data.sync

/** What the photo step of a sync does. */
enum class PhotoAction { NOTHING, UPLOAD, DOWNLOAD, REMOVE }

/**
 * The profile photo travels apart from the profile row (it is far bigger than all other
 * data), but the row decides: its photoUpdatedAt says which photo is current, and the row
 * already synced by the usual newest-change-wins rule. This only brings file and cloud in line.
 */
object PhotoSyncRules {
    /**
     * @param rowVersion the profile row's photoUpdatedAt after this sync's download
     * @param fileVersion the version of the photo file on this phone
     * @param cloudVersion the version this phone knows is in the cloud (uploaded or downloaded)
     */
    fun decide(rowVersion: Long?, fileVersion: Long?, cloudVersion: Long?): PhotoAction = when {
        // No photo any more: remove whatever is left here or in the cloud.
        rowVersion == null -> if (fileVersion != null || cloudVersion != null) PhotoAction.REMOVE else PhotoAction.NOTHING
        // This phone holds the current photo: make sure the cloud has it too.
        fileVersion == rowVersion -> if (cloudVersion == rowVersion) PhotoAction.NOTHING else PhotoAction.UPLOAD
        // The row names a photo this phone does not have: it was set on another phone.
        else -> PhotoAction.DOWNLOAD
    }
}
