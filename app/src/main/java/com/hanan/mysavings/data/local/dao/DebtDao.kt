package com.hanan.mysavings.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hanan.mysavings.data.local.entity.DebtEntity
import com.hanan.mysavings.data.local.entity.DebtPaymentEntity
import com.hanan.mysavings.data.local.entity.DebtReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    /** Each debt with the total paid, summed in SQL (same LEFT JOIN pattern as SavingsDao). */
    @Query(
        """
        SELECT debts.*, COALESCE(SUM(debt_payments.amount), 0) AS paid
        FROM debts
        LEFT JOIN debt_payments ON debt_payments.debtId = debts.id
        GROUP BY debts.id
        """,
    )
    fun observeDebtsWithPaid(): Flow<List<DebtWithPaidRow>>

    // One-shot twin of the query above, for the background reminder check.
    @Query(
        """
        SELECT debts.*, COALESCE(SUM(debt_payments.amount), 0) AS paid
        FROM debts
        LEFT JOIN debt_payments ON debt_payments.debtId = debts.id
        GROUP BY debts.id
        """,
    )
    suspend fun getDebtsWithPaid(): List<DebtWithPaidRow>

    @Query("SELECT * FROM debts WHERE id = :id")
    fun observeDebt(id: Long): Flow<DebtEntity?>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebt(id: Long): DebtEntity?

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY date DESC, createdAt DESC")
    fun observePayments(debtId: Long): Flow<List<DebtPaymentEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM debt_payments WHERE debtId = :debtId")
    suspend fun getPaid(debtId: Long): Long

    @Insert
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Insert
    suspend fun insertPayment(payment: DebtPaymentEntity): Long

    // Payments and sent reminders go with it through their CASCADE foreign keys.
    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebt(id: Long)

    @Query("DELETE FROM debt_payments WHERE id = :id")
    suspend fun deletePayment(id: Long)

    @Query(
        "SELECT EXISTS(SELECT 1 FROM debt_reminders_sent WHERE debtId = :debtId AND installmentNumber = :installmentNumber AND kind = :kind)",
    )
    suspend fun reminderSent(debtId: Long, installmentNumber: Int, kind: String): Boolean

    // IGNORE: marking an already-sent reminder again is harmless and keeps the original time.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReminder(reminder: DebtReminderEntity)

    @Query("DELETE FROM debt_reminders_sent WHERE debtId = :debtId")
    suspend fun clearReminders(debtId: Long)
}

/** Result row of [DebtDao.observeDebtsWithPaid]. */
data class DebtWithPaidRow(
    @Embedded val debt: DebtEntity,
    val paid: Long,
)
