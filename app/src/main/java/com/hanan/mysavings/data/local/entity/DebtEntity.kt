package com.hanan.mysavings.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creditor: String,
    /** Whole rupiah. */
    val totalAmount: Long,
    /** `LocalDate.toEpochDay()` of the first (or only) installment. */
    val firstDueDate: Long,
    /** 1 for a debt paid in one go. */
    val installmentCount: Int,
    val note: String?,
    /** Epoch millis. */
    val createdAt: Long,
)
