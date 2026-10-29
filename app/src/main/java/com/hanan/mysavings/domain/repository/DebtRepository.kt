package com.hanan.mysavings.domain.repository

import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.model.DebtPayment
import com.hanan.mysavings.domain.model.DebtWithPaid
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    /** Every debt with the total paid towards it. */
    fun observeDebtsWithPaid(): Flow<List<DebtWithPaid>>

    /** The debt, or `null` once it is deleted. */
    fun observeDebt(id: Long): Flow<Debt?>

    /** Payments of one debt, newest first. */
    fun observePayments(debtId: Long): Flow<List<DebtPayment>>

    suspend fun getDebt(id: Long): Debt?

    suspend fun getDebtsWithPaid(): List<DebtWithPaid>

    /** Total paid towards a debt. */
    suspend fun getPaid(debtId: Long): Long

    /** Inserts when [Debt.id] is 0, otherwise updates. Returns the debt's id. */
    suspend fun saveDebt(debt: Debt): Long

    suspend fun addPayment(payment: DebtPayment): Long

    /** Deletes the debt and, with it, its payments and sent reminders. */
    suspend fun deleteDebt(id: Long)

    suspend fun deletePayment(id: Long)
}

