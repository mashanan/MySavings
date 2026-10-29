package com.hanan.strukku.ui.home

import app.cash.turbine.test
import com.hanan.strukku.domain.model.MonthlySummary
import com.hanan.strukku.domain.usecase.ObserveMonthlySummaryUseCase
import com.hanan.strukku.domain.usecase.SetBudgetUseCase
import com.hanan.strukku.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val october = YearMonth.of(2026, 10)
    private val clock = Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"), ZoneId.of("Asia/Jakarta"))

    private val summaries = MutableSharedFlow<MonthlySummary>()
    private val observeSummary = mockk<ObserveMonthlySummaryUseCase> {
        every { this@mockk.invoke(october) } returns summaries
    }
    private val setBudget = mockk<SetBudgetUseCase>()

    private fun createViewModel() = HomeViewModel(observeSummary, setBudget, clock)

    @Test
    fun `starts loading then shows the current month summary`() = runTest {
        val viewModel = createViewModel()
        val summary = MonthlySummary(october, 100_000, 1_000_000, emptyList(), emptyList())

        viewModel.uiState.test {
            assertEquals(HomeUiState.Loading, awaitItem())

            summaries.emit(summary)

            assertEquals(HomeUiState.Content(summary), awaitItem())
        }
    }

    @Test
    fun `saving a budget sets the overall budget for the current month`() = runTest {
        coEvery { setBudget(any(), any(), any()) } returns true

        createViewModel().onSaveBudget(2_500_000)

        coVerify { setBudget(october, null, 2_500_000) }
    }
}
