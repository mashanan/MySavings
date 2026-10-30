package com.hanan.mysavings.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "debts", indices = [Index(value = ["uuid"], unique = true)])
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
    @Embedded val sync: SyncColumns = SyncColumns(updatedAt = createdAt),
)
