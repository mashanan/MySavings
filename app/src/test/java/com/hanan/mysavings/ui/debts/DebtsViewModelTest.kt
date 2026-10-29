package com.hanan.mysavings.ui.debts

import app.cash.turbine.test
import com.hanan.mysavings.domain.model.Debt
import com.hanan.mysavings.domain.model.DebtStatus
import com.hanan.mysavings.domain.usecase.DebtWithStatus
import com.hanan.mysavings.domain.usecase.DebtsOverview
import com.hanan.mysavings.domain.usecase.DueItem
import com.hanan.mysavings.domain.usecase.ObserveDebtsUseCase
import com.hanan.mysavings.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class DebtsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 29)
    private val clock = Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val dueDate = LocalDate.of(2026, 11, 5)
    private val debt = Debt(1, "Budi", 500_000, dueDate, 1, null, Instant.EPOCH)
    private val status = DebtStatus.of(debt, 0, today)
    private val dueItem = DueItem(1, "Budi", status.installments.single(), 1)

    private val observeDebts = mockk<ObserveDebtsUseCase> {
        every { this@mockk.invoke() } returns flowOf(
            DebtsOverview(500_000, 0, listOf(DebtWithStatus(debt, status)), emptyList(), mapOf(dueDate to listOf(dueItem))),
        )
    }

    private fun viewModel() = DebtsViewModel(observeDebts, clock)

    @Test
    fun `starts on this month with today selected`() = runTest {
        viewModel().uiState.test {
            val state = awaitItem() as DebtsUiState.Content

            assertEquals(YearMonth.of(2026, 10), state.month)
            assertEquals(today, state.selectedDate)
            assertTrue(state.dueOnSelectedDate.isEmpty())
            assertTrue(state.dots.isEmpty()) // the only due date is in November
            assertTrue(state.hasDebts)
        }
    }

    @Test
    fun `next month shows its dots and clears a selection from another month`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.onNextMonth()

            val state = awaitItem() as DebtsUiState.Content
            assertEquals(YearMonth.of(2026, 11), state.month)
            assertNull(state.selectedDate)
            assertEquals(DueDot.SOON, state.dots[dueDate])
        }
    }

    @Test
    fun `selecting a day lists what is due, selecting it again clears it`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.onSelectDate(dueDate)
            assertEquals(listOf(dueItem), (awaitItem() as DebtsUiState.Content).dueOnSelectedDate)

            viewModel.onSelectDate(dueDate)
            assertNull((awaitItem() as DebtsUiState.Content).selectedDate)
        }
    }

    @Test
    fun `coming back to this month selects today again`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitItem()
            viewModel.onPreviousMonth()
            awaitItem()
            viewModel.onNextMonth()

            assertEquals(today, (awaitItem() as DebtsUiState.Content).selectedDate)
        }
    }
}
