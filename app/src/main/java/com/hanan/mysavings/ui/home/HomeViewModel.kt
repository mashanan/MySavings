package com.hanan.mysavings.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.model.MonthInsights
import com.hanan.mysavings.domain.model.MonthlySummary
import com.hanan.mysavings.domain.usecase.DayTotal
import com.hanan.mysavings.domain.usecase.ObserveDailyTotalsUseCase
import com.hanan.mysavings.domain.usecase.ObserveMonthInsightsUseCase
import com.hanan.mysavings.domain.usecase.ObserveMonthlySummaryUseCase
import com.hanan.mysavings.domain.usecase.ObserveTransactionsByDayUseCase
import com.hanan.mysavings.domain.usecase.SetBudgetUseCase
import com.hanan.mysavings.domain.usecase.TransactionDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeMonthlySummary: ObserveMonthlySummaryUseCase,
    observeMonthInsights: ObserveMonthInsightsUseCase,
    observeDailyTotals: ObserveDailyTotalsUseCase,
    observeTransactionsByDay: ObserveTransactionsByDayUseCase,
    private val setBudget: SetBudgetUseCase,
    private val clock: Clock,
) : ViewModel() {

    // The date is state, not a constant: the app can stay open across midnight, and the
    // daily allowance changes every day while the month (and its summary) changes rarely.
    private val today = MutableStateFlow(LocalDate.now(clock))

    // null means "follow the current month", so Home moves on to November by itself on the
    // 1st, but a user who deliberately went back to August stays there.
    private val selectedMonth = MutableStateFlow<YearMonth?>(null)

    private val homeDate = combine(today, selectedMonth) { date, selected -> HomeDate(date, selected) }
        .distinctUntilChanged()

    // Each query is keyed only by what it depends on, and distinctUntilChanged stops it from
    // restarting when something else changes: a new day in the same month keeps the summary
    // query, and browsing past months keeps nothing it does not need.
    // flatMapLatest cancels the previous query when its key changes.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthData = homeDate.map { it.month }.distinctUntilChanged().flatMapLatest { month ->
        combine(observeMonthlySummary(month), observeTransactionsByDay(month)) { summary, days -> summary to days }
    }

    // Insights build on the summary, so they are recomputed whenever it changes. The transaction
    // list rides along with its summary: both belong to the same month and change together.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthWithInsights = combine(monthData, homeDate) { data, date -> data to date.today }
        .distinctUntilChanged()
        .flatMapLatest { (data, today) ->
            val (summary, days) = data
            observeMonthInsights(summary, today).map { MonthContent(summary, days, it) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dailyTotals = homeDate.map { it.chartEnd }.distinctUntilChanged().flatMapLatest { observeDailyTotals(it) }

    // WhileSubscribed(5s) keeps the queries alive across rotation but stops them in the background.
    val uiState: StateFlow<HomeUiState> = combine(monthWithInsights, dailyTotals, homeDate) { (summary, transactionDays, insights), daily, date ->
        // While switching months the queries answer one by one; skip the frames that still mix in the old month.
        if (summary.yearMonth != date.month || daily.lastOrNull()?.date != date.chartEnd) return@combine null
        HomeUiState.Content(
            summary = summary,
            insights = insights,
            transactionDays = transactionDays,
            dailyTotals = daily,
            today = date.today,
            canGoNext = date.month < date.currentMonth,
        )
    }
        .filterNotNull()
        .stateIn<HomeUiState>(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    /** Called when the screen comes back to the foreground. A no-op unless the date changed. */
    fun refreshDate() {
        today.value = LocalDate.now(clock)
    }

    fun onPreviousMonth() {
        selectedMonth.value = displayedMonth().minusMonths(1)
    }

    fun onNextMonth() {
        val next = displayedMonth().plusMonths(1)
        val current = YearMonth.from(today.value)
        // Reaching the current month goes back to following it.
        selectedMonth.value = if (next >= current) null else next
    }

    /** Sets the overall budget of the month on screen, which can be a past one. */
    fun onSaveBudget(limitAmount: Long) {
        val month = displayedMonth()
        viewModelScope.launch { setBudget(month, categoryId = null, limitAmount = limitAmount) }
    }

    private fun displayedMonth(): YearMonth = selectedMonth.value ?: YearMonth.from(today.value)

    private data class MonthContent(
        val summary: MonthlySummary,
        val transactionDays: List<TransactionDay>,
        val insights: MonthInsights,
    )

    /** Which month Home shows and which day its chart ends on. */
    private data class HomeDate(val today: LocalDate, val selected: YearMonth?) {
        val currentMonth: YearMonth get() = YearMonth.from(today)
        val month: YearMonth get() = selected ?: currentMonth

        /** The chart shows the week up to today, or the last week of a past month. */
        val chartEnd: LocalDate get() = if (month == currentMonth) today else month.atEndOfMonth()
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(
        val summary: MonthlySummary,
        val insights: MonthInsights,
        /** The month's transactions grouped per day, newest first. */
        val transactionDays: List<TransactionDay>,
        /** The last seven days of the chart, oldest first. */
        val dailyTotals: List<DayTotal>,
        val today: LocalDate,
        val canGoNext: Boolean,
    ) : HomeUiState
}
