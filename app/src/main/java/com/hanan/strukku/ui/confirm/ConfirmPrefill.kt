package com.hanan.strukku.ui.confirm

import com.hanan.strukku.domain.model.ReceiptDraft
import com.hanan.strukku.domain.model.ReceiptField
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Initial values for the Confirm form, passed as a navigation argument.
 *
 * Plain serializable values only (no LocalDate, no Category), because navigation arguments
 * are written into the back stack and restored after process death.
 */
@Serializable
data class ConfirmPrefill(
    val merchant: String? = null,
    val total: Long? = null,
    val dateEpochDay: Long? = null,
    val categoryId: Long? = null,
    /** Fields the scan could not read (or only guessed) that the user should double-check. */
    val needsCheck: Set<ReceiptField> = emptySet(),
    val fromScan: Boolean = false,
) {
    val date: LocalDate? get() = dateEpochDay?.let(LocalDate::ofEpochDay)

    companion object {
        /** Empty form for manual entry. */
        val Manual = ConfirmPrefill()

        fun fromDraft(draft: ReceiptDraft) = ConfirmPrefill(
            merchant = draft.merchant,
            total = draft.total,
            dateEpochDay = draft.date?.toEpochDay(),
            categoryId = draft.category?.id,
            needsCheck = draft.missingFields,
            fromScan = true,
        )
    }
}
