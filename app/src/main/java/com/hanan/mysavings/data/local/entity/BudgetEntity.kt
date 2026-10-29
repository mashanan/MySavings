package com.hanan.mysavings.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // Note: SQLite treats NULLs as distinct in UNIQUE indexes, so this does not stop two
    // overall (categoryId = NULL) budgets in one month. BudgetDao.upsert guards that case.
    indices = [Index(value = ["yearMonth", "categoryId"], unique = true), Index("categoryId")],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** year * 100 + month, e.g. 202610. */
    val yearMonth: Int,
    /** `null` means the overall monthly budget. */
    val categoryId: Long?,
    val limitAmount: Long,
)
