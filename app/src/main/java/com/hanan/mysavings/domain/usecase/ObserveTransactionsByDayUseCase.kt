package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.model.TransactionWithCategory
import com.hanan.mysavings.domain.repository.CategoryRepository
import com.hanan.mysavings.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** One day of the Home transaction list. */
data class TransactionDay(
    val date: LocalDate,
    val total: Long,
    /** Newest first. */
    val transactions: List<TransactionWithCategory>,
)

/** All transactions of a month grouped per day, newest day first, each joined with its category. */
class ObserveTransactionsByDayUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(yearMonth: YearMonth): Flow<List<TransactionDay>> = combine(
        transactionRepository.observeInRange(yearMonth.atDay(1), yearMonth.atEndOfMonth()),
        categoryRepository.observeAll(),
    ) { transactions, categories -> groupByDay(transactions, categories) }

    companion object {
        /**
         * Expects [transactions] newest first, as the repository returns them; groupBy keeps
         * that order, so neither the days nor the rows inside a day need sorting again.
         * A transaction whose category is missing is skipped rather than shown without one.
         */
        fun groupByDay(transactions: List<Transaction>, categories: List<Category>): List<TransactionDay> {
            val categoriesById = categories.associateBy { it.id }
            return transactions
                .mapNotNull { transaction -> categoriesById[transaction.categoryId]?.let { TransactionWithCategory(transaction, it) } }
                .groupBy { it.transaction.date }
                .map { (date, items) -> TransactionDay(date, items.sumOf { it.transaction.amount }, items) }
        }
    }
}
