package com.hanan.mysavings.data.repository

import com.hanan.mysavings.data.local.dao.DebtDao
import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.model.DebtPayment
import com.hanan.mysavings.domain.model.DebtWithPaid
import com.hanan.mysavings.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

class RoomDebtRepository @Inject constructor(
    private val dao: DebtDao,
    private val clock: Clock,
) : DebtRepository {

    override fun observeDebtsWithPaid(): Flow<List<DebtWithPaid>> =
        dao.observeDebtsWithPaid().map { rows -> rows.map { DebtWithPaid(it.debt.toDomain(), it.paid) } }

    override fun observeDebt(id: Long): Flow<Debt?> = dao.observeDebt(id).map { it?.toDomain() }

    override fun observePayments(debtId: Long): Flow<List<DebtPayment>> =
        dao.observePayments(debtId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getDebt(id: Long): Debt? = dao.getDebt(id)?.toDomain()

    override suspend fun getDebtsWithPaid(): List<DebtWithPaid> =
        dao.getDebtsWithPaid().map { DebtWithPaid(it.debt.toDomain(), it.paid) }

    override suspend fun getPaid(debtId: Long): Long = dao.getPaid(debtId)

    override suspend fun saveDebt(debt: Debt): Long =
        if (debt.id == 0L) {
            dao.insertDebt(debt.toEntity())
        } else {
            // Keep the row's uuid: it is how other phones and the cloud know this debt.
            val existing = checkNotNull(dao.getDebt(debt.id)) { "Debt ${debt.id} not found" }
            dao.updateDebt(debt.toEntity().copy(sync = existing.sync.touched(clock.millis())))
            debt.id
        }

    override suspend fun addPayment(payment: DebtPayment): Long = dao.insertPayment(payment.toEntity())

    override suspend fun deleteDebt(id: Long) = dao.deleteDebtTracked(id, clock.millis())

    override suspend fun deletePayment(id: Long) = dao.deletePaymentTracked(id, clock.millis())
}
