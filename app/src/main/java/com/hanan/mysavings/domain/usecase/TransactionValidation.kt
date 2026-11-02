package com.hanan.mysavings.domain.usecase

import java.time.LocalDate

/**
 * The rules every saved expense must follow, shared by [AddTransactionUseCase] and
 * [UpdateTransactionUseCase] so adding and editing can never drift apart.
 *
 * @return the first broken rule, or null when the values are valid.
 */
internal fun validateTransaction(amount: Long, date: LocalDate, today: LocalDate): TransactionError? = when {
    amount <= 0 -> TransactionError.AMOUNT_NOT_POSITIVE
    date.isAfter(today) -> TransactionError.DATE_IN_FUTURE
    else -> null
}

/** Result of adding or editing a transaction. */
sealed interface SaveTransactionResult {
    data class Success(val id: Long) : SaveTransactionResult
    data class Invalid(val error: TransactionError) : SaveTransactionResult
}

enum class TransactionError {
    AMOUNT_NOT_POSITIVE,
    DATE_IN_FUTURE,

    /** The transaction being edited was deleted meanwhile, e.g. on another phone through sync. */
    NOT_FOUND,
}
