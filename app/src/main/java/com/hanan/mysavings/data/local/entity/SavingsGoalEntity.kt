package com.hanan.mysavings.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals", indices = [Index(value = ["uuid"], unique = true)])
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Whole rupiah. */
    val targetAmount: Long,
    /** `GoalCategory.key`. */
    val categoryKey: String,
    /** `LocalDate.toEpochDay()`, or null without a deadline. */
    val targetDate: Long?,
    /** Epoch millis. */
    val createdAt: Long,
    @Embedded val sync: SyncColumns = SyncColumns(updatedAt = createdAt),
)
