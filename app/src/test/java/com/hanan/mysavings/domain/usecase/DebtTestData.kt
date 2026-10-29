package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.testutil.MutableClock
import java.time.Instant
import java.time.LocalDate

/** Shared fixtures for the debt tests. "Today" is 29 October 2026 in Jakarta. */
internal object DebtTestData {
    val today: LocalDate = LocalDate.of(2026, 10, 29)

    fun clock() = MutableClock(Instant.parse("2026-10-29T03:00:00Z"))

    fun debt(
        id: Long = 1,
        creditor: String = "Leasing",
        total: Long = 1_200_000,
        firstDue: LocalDate = LocalDate.of(2026, 11, 5),
        count: Int = 1,
        createdAt: Instant = Instant.EPOCH,
    ) = Debt(id, creditor, total, firstDue, count, null, createdAt)
}
