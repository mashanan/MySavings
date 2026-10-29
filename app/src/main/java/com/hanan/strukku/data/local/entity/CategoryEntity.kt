package com.hanan.strukku.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    /** Comma-separated lowercase keywords. A plain string avoids a join table for a short list. */
    val keywords: String,
    val isDefault: Boolean,
)
