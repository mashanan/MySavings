package com.hanan.mysavings.domain.repository

/**
 * The account's data as a whole, on this phone and in the cloud: what sign-out with
 * "erase" and account deletion need. How sync and storage do it is the data layer's business.
 */
interface AccountDataManager {
    /**
     * Tries one last sync, so as little as possible is lost by erasing this phone.
     * @return how many changes are still only on this phone afterwards (0 = nothing to lose).
     */
    suspend fun syncBeforeErase(): Int

    /** Forgets the account's data on this phone only; the cloud and other phones keep theirs. */
    suspend fun eraseLocalData()

    /** Deletes everything stored in the cloud for the signed-in account. */
    suspend fun deleteCloudData(): Boolean
}
