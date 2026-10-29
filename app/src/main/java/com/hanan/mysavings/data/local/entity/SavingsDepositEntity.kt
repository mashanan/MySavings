package com.hanan.mysavings.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "savings_deposits",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            // Deposits mean nothing without their goal, so deleting a goal removes them too.
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("goalId")],
)
data class SavingsDepositEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    /** Whole rupiah; negative for a withdrawal. */
    val amount: Long,
    /** `LocalDate.toEpochDay()`. */
    val date: Long,
    val note: String?,
    /** Epoch millis. */
    val createdAt: Long,
)
