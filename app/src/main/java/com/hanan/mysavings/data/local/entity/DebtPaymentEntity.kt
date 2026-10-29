package com.hanan.mysavings.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            // Payments mean nothing without their debt, so deleting a debt removes them too.
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("debtId")],
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    /** Whole rupiah, always positive. */
    val amount: Long,
    /** `LocalDate.toEpochDay()`. */
    val date: Long,
    val note: String?,
    /** Epoch millis. */
    val createdAt: Long,
)
