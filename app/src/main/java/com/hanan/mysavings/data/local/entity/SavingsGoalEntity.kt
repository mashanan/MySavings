package com.hanan.mysavings.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
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
)
