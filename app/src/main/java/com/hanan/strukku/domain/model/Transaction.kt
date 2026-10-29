package com.hanan.strukku.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * A single expense.
 *
 * @property amount whole rupiah. Always positive.
 * @property date the day the purchase happened (from the receipt or picked by the user).
 * @property createdAt when the record was saved, used to order same-day transactions.
 */
data class Transaction(
    val id: Long = 0,
    val amount: Long,
    val date: LocalDate,
    val merchant: String?,
    val note: String?,
    val categoryId: Long,
    val source: TransactionSource,
    val createdAt: Instant,
)

enum class TransactionSource { MANUAL, SCAN }
