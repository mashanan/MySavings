package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.repository.DebtReminderRepository
import com.hanan.mysavings.domain.repository.DebtRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Creates a debt (`id == null`) or updates an existing one. */
class SaveDebtUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
    private val reminderRepository: DebtReminderRepository,
    private val clock: Clock,
) {
    /**
     * A first due date in the past is allowed on purpose: people often start tracking an
     * installment plan that began months ago.
     */
    suspend operator fun invoke(
        id: Long?,
        creditor: String,
        totalAmount: Long,
        firstDueDate: LocalDate,
        installmentCount: Int,
        note: String?,
    ): SaveDebtResult {
        val trimmedCreditor = creditor.trim()
        val existing = id?.let { debtRepository.getDebt(it) ?: return SaveDebtResult.Invalid(DebtError.NOT_FOUND) }

        val errors = buildSet {
            if (trimmedCreditor.isEmpty()) add(DebtError.CREDITOR_BLANK)
            if (totalAmount <= 0) add(DebtError.AMOUNT_NOT_POSITIVE)
            if (installmentCount !in 1..MAX_INSTALLMENTS) add(DebtError.INSTALLMENTS_OUT_OF_RANGE)
            // Lowering the total below what was already paid would leave a negative balance.
            if (existing != null && totalAmount > 0 && totalAmount < debtRepository.getPaid(existing.id)) {
                add(DebtError.AMOUNT_BELOW_PAID)
            }
        }
        if (errors.isNotEmpty()) return SaveDebtResult.Invalid(errors)

        val savedId = debtRepository.saveDebt(
            Debt(
                id = existing?.id ?: 0,
                creditor = trimmedCreditor,
                totalAmount = totalAmount,
                firstDueDate = firstDueDate,
                installmentCount = installmentCount,
                note = note?.trim()?.ifBlank { null },
                createdAt = existing?.createdAt ?: Instant.now(clock),
            ),
        )
        // Reminders are stored per installment number; after an edit the numbers may point at
        // other dates or amounts, so start the reminders afresh.
        if (existing != null) reminderRepository.clear(savedId)
        return SaveDebtResult.Success(savedId)
    }

    companion object {
        /** 30 years of monthly installments, enough for a mortgage. */
        const val MAX_INSTALLMENTS = 360
    }
}

sealed interface SaveDebtResult {
    data class Success(val id: Long) : SaveDebtResult

    /** All problems at once, so the form can mark every field in one go. */
    data class Invalid(val errors: Set<DebtError>) : SaveDebtResult {
        constructor(error: DebtError) : this(setOf(error))
    }
}

enum class DebtError { CREDITOR_BLANK, AMOUNT_NOT_POSITIVE, INSTALLMENTS_OUT_OF_RANGE, AMOUNT_BELOW_PAID, NOT_FOUND }
