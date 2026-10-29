package com.hanan.mysavings.data.repository

import com.hanan.mysavings.data.local.dao.BudgetAlertDao
import com.hanan.mysavings.data.local.entity.BudgetAlertEntity
import com.hanan.mysavings.domain.model.BudgetAlertLevel
import com.hanan.mysavings.domain.repository.BudgetAlertRepository
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

class RoomBudgetAlertRepository @Inject constructor(
    private val dao: BudgetAlertDao,
    private val clock: Clock,
) : BudgetAlertRepository {

    override suspend fun sentLevels(yearMonth: YearMonth): Set<BudgetAlertLevel> {
        val sent = dao.sentThresholds(yearMonth.toInt()).toSet()
        return BudgetAlertLevel.entries.filter { it.percent in sent }.toSet()
    }

    override suspend fun markSent(yearMonth: YearMonth, levels: Set<BudgetAlertLevel>) {
        val now = clock.millis()
        dao.insertAll(levels.map { BudgetAlertEntity(yearMonth.toInt(), it.percent, now) })
    }

    override suspend fun clear(yearMonth: YearMonth) = dao.clear(yearMonth.toInt())
}
