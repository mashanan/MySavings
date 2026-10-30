package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.SyncStatus
import com.hanan.mysavings.domain.repository.SyncController
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSyncStatusUseCase @Inject constructor(
    private val syncController: SyncController,
) {
    operator fun invoke(): Flow<SyncStatus> = syncController.observeStatus()
}
