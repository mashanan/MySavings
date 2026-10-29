package com.hanan.strukku.domain.model

import java.time.YearMonth

/**
 * A monthly spending limit.
 *
 * @property categoryId `null` for the overall monthly budget, otherwise a per-category budget.
 * @property limitAmount whole rupiah. Always positive.
 */
data class Budget(
    val id: Long = 0,
    val yearMonth: YearMonth,
    val categoryId: Long?,
    val limitAmount: Long,
)
