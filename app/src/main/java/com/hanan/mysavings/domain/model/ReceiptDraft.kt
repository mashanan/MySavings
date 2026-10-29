package com.hanan.mysavings.domain.model

import java.time.LocalDate

/**
 * What could be read from a receipt photo. Every field may be missing; the user reviews
 * and completes the draft on the Confirm screen before anything is saved.
 *
 * @property category the guessed category, or the fallback "Lainnya" when nothing matched.
 * @property missingFields fields that were not detected (or only guessed) and should be checked.
 */
data class ReceiptDraft(
    val merchant: String?,
    val total: Long?,
    val date: LocalDate?,
    val category: Category?,
    val missingFields: Set<ReceiptField>,
) {
    /** True when nothing useful was found, i.e. the photo is probably not a readable receipt. */
    val isEmpty: Boolean get() = merchant == null && total == null && date == null
}

enum class ReceiptField { MERCHANT, TOTAL, DATE, CATEGORY }
