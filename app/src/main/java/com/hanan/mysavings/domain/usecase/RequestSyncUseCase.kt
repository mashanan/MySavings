package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.SyncController
import javax.inject.Inject

/** The "Sync now" button. Automatic syncs are started by the data layer itself. */
class RequestSyncUseCase @Inject constructor(
    private val syncController: SyncController,
) {
    operator fun invoke() = syncController.requestSync()
}
