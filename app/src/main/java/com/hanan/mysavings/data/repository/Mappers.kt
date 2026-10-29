package com.hanan.mysavings.data.repository

import com.hanan.mysavings.data.local.entity.BudgetEntity
import com.hanan.mysavings.data.local.entity.CategoryEntity
import com.hanan.mysavings.data.local.entity.TransactionEntity
import com.hanan.mysavings.domain.model.Budget
import com.hanan.mysavings.domain.model.Category
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

/** 2026-10 -> 202610. Sortable and readable in the database. */
fun YearMonth.toInt(): Int = year * 100 + monthValue

fun Int.toYearMonth(): YearMonth = YearMonth.of(this / 100, this % 100)
