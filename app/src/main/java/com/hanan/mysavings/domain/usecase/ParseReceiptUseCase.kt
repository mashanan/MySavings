package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.ReceiptDraft
import com.hanan.mysavings.domain.model.ReceiptField
import com.hanan.mysavings.domain.parser.CategoryClassifier
import com.hanan.mysavings.domain.parser.ReceiptParser
import com.hanan.mysavings.domain.parser.ReceiptRowBuilder
import com.hanan.mysavings.domain.parser.TextFragment
import com.hanan.mysavings.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Turns raw OCR fragments into a [ReceiptDraft]: rows -> merchant/total/date -> category. */
class ParseReceiptUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(fragments: List<TextFragment>): ReceiptDraft {
        val rows = ReceiptRowBuilder.buildRows(fragments)
        val parsed = ReceiptParser.parse(rows, today = LocalDate.now(clock))

        val categories = categoryRepository.observeAll().first()
        val matched = CategoryClassifier.classify(parsed.merchant, rows, categories)

        return ReceiptDraft(
            merchant = parsed.merchant,
            total = parsed.total,
            date = parsed.date,
            category = matched ?: categories.firstOrNull { it.iconKey == Category.OTHER_ICON_KEY },
            missingFields = buildSet {
                if (parsed.merchant == null) add(ReceiptField.MERCHANT)
                if (parsed.total == null) add(ReceiptField.TOTAL)
                if (parsed.date == null) add(ReceiptField.DATE)
                // A fallback category is only a guess, so the user should check it.
                if (matched == null) add(ReceiptField.CATEGORY)
            },
        )
    }
}
