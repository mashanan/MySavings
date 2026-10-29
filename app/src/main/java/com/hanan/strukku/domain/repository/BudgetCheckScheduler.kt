package com.hanan.strukku.domain.repository

/**
 * Asks for a budget check to run soon, in the background. The domain only states the need;
 * how it runs (WorkManager) is an implementation detail of the outer layers.
 */
fun interface BudgetCheckScheduler {
    fun requestCheck()
}
