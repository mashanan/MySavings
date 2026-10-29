package com.hanan.strukku.domain.model

/**
 * A spending category.
 *
 * @property keywords lowercase words used to guess this category from a receipt (phase 4).
 */
data class Category(
    val id: Long,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val keywords: List<String>,
    val isDefault: Boolean,
)
