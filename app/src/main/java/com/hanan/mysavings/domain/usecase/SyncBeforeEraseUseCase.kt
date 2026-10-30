package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.repository.AccountDataManager
import javax.inject.Inject

/** Before erasing this phone: one last sync, and how many changes would still be lost. */
class SyncBeforeEraseUseCase @Inject constructor(
    private val accountData: AccountDataManager,
) {
    suspend operator fun invoke(): Int = accountData.syncBeforeErase()
}
