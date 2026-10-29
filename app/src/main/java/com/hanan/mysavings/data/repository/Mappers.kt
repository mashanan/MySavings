package com.hanan.mysavings.data.repository

import com.hanan.mysavings.data.local.entity.BudgetEntity
import com.hanan.mysavings.data.local.entity.CategoryEntity
import com.hanan.mysavings.data.local.entity.DebtEntity
import com.hanan.mysavings.data.local.entity.DebtPaymentEntity
import com.hanan.mysavings.data.local.entity.SavingsDepositEntity
import com.hanan.mysavings.data.local.entity.SavingsGoalEntity
import com.hanan.mysavings.data.local.entity.TransactionEntity
import com.hanan.mysavings.data.local.entity.UserProfileEntity
import com.hanan.mysavings.domain.model.Budget
import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.model.DebtPayment
import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.model.ProfileRecord
import com.hanan.mysavings.domain.model.SavingsDeposit
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.model.Transaction
import com.hanan.mysavings.domain.model.TransactionSource
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

// Entity <-> domain conversion lives in one place, so Room types never leak out of the data layer.

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    keywords = keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    isDefault = isDefault,
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    amount = amount,
    date = LocalDate.ofEpochDay(date),
    merchant = merchant,
    note = note,
    categoryId = categoryId,
    source = TransactionSource.valueOf(source),
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    amount = amount,
    date = date.toEpochDay(),
    merchant = merchant,
    note = note,
    categoryId = categoryId,
    source = source.name,
    createdAt = createdAt.toEpochMilli(),
)

fun BudgetEntity.toDomain() = Budget(
    id = id,
    yearMonth = yearMonth.toYearMonth(),
    categoryId = categoryId,
    limitAmount = limitAmount,
)

fun Budget.toEntity() = BudgetEntity(
    id = id,
    yearMonth = yearMonth.toInt(),
    categoryId = categoryId,
    limitAmount = limitAmount,
)

fun SavingsGoalEntity.toDomain() = SavingsGoal(
    id = id,
    name = name,
    targetAmount = targetAmount,
    category = GoalCategory.fromKey(categoryKey),
    targetDate = targetDate?.let { LocalDate.ofEpochDay(it) },
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun SavingsGoal.toEntity() = SavingsGoalEntity(
    id = id,
    name = name,
    targetAmount = targetAmount,
    categoryKey = category.key,
    targetDate = targetDate?.toEpochDay(),
    createdAt = createdAt.toEpochMilli(),
)

fun SavingsDepositEntity.toDomain() = SavingsDeposit(
    id = id,
    goalId = goalId,
    amount = amount,
    date = LocalDate.ofEpochDay(date),
    note = note,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun SavingsDeposit.toEntity() = SavingsDepositEntity(
    id = id,
    goalId = goalId,
    amount = amount,
    date = date.toEpochDay(),
    note = note,
    createdAt = createdAt.toEpochMilli(),
)

fun DebtEntity.toDomain() = Debt(
    id = id,
    creditor = creditor,
    totalAmount = totalAmount,
    firstDueDate = LocalDate.ofEpochDay(firstDueDate),
    installmentCount = installmentCount,
    note = note,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun Debt.toEntity() = DebtEntity(
    id = id,
    creditor = creditor,
    totalAmount = totalAmount,
    firstDueDate = firstDueDate.toEpochDay(),
    installmentCount = installmentCount,
    note = note,
    createdAt = createdAt.toEpochMilli(),
)

fun DebtPaymentEntity.toDomain() = DebtPayment(
    id = id,
    debtId = debtId,
    amount = amount,
    date = LocalDate.ofEpochDay(date),
    note = note,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun DebtPayment.toEntity() = DebtPaymentEntity(
    id = id,
    debtId = debtId,
    amount = amount,
    date = date.toEpochDay(),
    note = note,
    createdAt = createdAt.toEpochMilli(),
)

fun UserProfileEntity.toDomain() = ProfileRecord(
    name = name,
    photoUpdatedAt = photoUpdatedAt?.let { Instant.ofEpochMilli(it) },
)

/** 2026-10 -> 202610. Sortable and readable in the database. */
fun YearMonth.toInt(): Int = year * 100 + monthValue

fun Int.toYearMonth(): YearMonth = YearMonth.of(this / 100, this % 100)
