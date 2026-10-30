package com.hanan.mysavings.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            // A category that still has transactions cannot be deleted by accident.
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    // categoryId: required for foreign keys to stay fast; date: monthly range queries.
    indices = [Index("categoryId"), Index("date"), Index(value = ["uuid"], unique = true)],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Whole rupiah. */
    val amount: Long,
    /** `LocalDate.toEpochDay()`, so a month is a simple numeric range. */
    val date: Long,
    val merchant: String?,
    val note: String?,
    val categoryId: Long,
    /** `TransactionSource.name`. */
    val source: String,
    /** Epoch millis. */
    val createdAt: Long,
    @Embedded val sync: SyncColumns = SyncColumns(updatedAt = createdAt),
)
