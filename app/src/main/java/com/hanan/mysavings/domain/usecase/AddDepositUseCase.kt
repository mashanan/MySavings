package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.SavingsDeposit
import com.hanan.mysavings.domain.repository.SavingsRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Puts money into a goal, or takes it out again. */
class AddDepositUseCase @Inject constructor(
    private val savingsRepository: SavingsRepository,
    private val clock: Clock,
) {
    /**
     * @param amount always positive; [isWithdrawal] decides the direction. The sign is applied
     * here, so callers never have to remember that withdrawals are stored as negative numbers.
     */
    suspend operator fun invoke(
        goalId: Long,
        amount: Long,
        isWithdrawal: Boolean,
        date: LocalDate,
        note: String?,
    ): AddDepositResult {
        if (amount <= 0) return AddDepositResult.Invalid(DepositError.AMOUNT_NOT_POSITIVE)
        if (date.isAfter(LocalDate.now(clock))) return AddDepositResult.Invalid(DepositError.DATE_IN_FUTURE)
        // A goal cannot go below zero: you cannot take out money that was never put in.
        if (isWithdrawal && amount > savingsRepository.getSaved(goalId)) {
            return AddDepositResult.Invalid(DepositError.EXCEEDS_BALANCE)
        }

        val id = savingsRepository.addDeposit(
            SavingsDeposit(
                goalId = goalId,
                amount = if (isWithdrawal) -amount else amount,
                date = date,
                note = note?.trim()?.ifBlank { null },
                createdAt = Instant.now(clock),
            ),
        )
        return AddDepositResult.Success(id)
    }
}

sealed interface AddDepositResult {
    data class Success(val id: Long) : AddDepositResult
    data class Invalid(val error: DepositError) : AddDepositResult
}

enum class DepositError { AMOUNT_NOT_POSITIVE, DATE_IN_FUTURE, EXCEEDS_BALANCE }
