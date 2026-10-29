package com.hanan.strukku.ui.confirm

import app.cash.turbine.test
import com.hanan.strukku.domain.model.Category
import com.hanan.strukku.domain.model.TransactionSource
import com.hanan.strukku.domain.usecase.AddTransactionResult
import com.hanan.strukku.domain.usecase.AddTransactionUseCase
import com.hanan.strukku.domain.usecase.ObserveCategoriesUseCase
import com.hanan.strukku.domain.usecase.TransactionError
import com.hanan.strukku.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ConfirmViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 29)
    private val clock = Clock.fixed(Instant.parse("2026-10-29T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val food = Category(1, "Makan & Minum", "food", "#E57373", emptyList(), true)

    private val observeCategories = mockk<ObserveCategoriesUseCase> {
        every { this@mockk.invoke() } returns flowOf(listOf(food))
    }
    private val addTransaction = mockk<AddTransactionUseCase>()

    private fun createViewModel() = ConfirmViewModel(observeCategories, addTransaction, clock)

    @Test
    fun `form starts empty with today's date and loads categories`() = runTest {
        createViewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals(ConfirmForm(date = today), state.form)
            assertEquals(listOf(food), state.categories)
            assertEquals(today, state.maxDate)
        }
    }

    @Test
    fun `saving with empty amount and no category shows both errors and saves nothing`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            viewModel.onSave()

            val form = expectMostRecentItem().form
            assertTrue(form.amountError)
            assertTrue(form.categoryError)
            assertFalse(form.isSaved)
        }
        coVerify(exactly = 0) { addTransaction(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `editing a field clears its error`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            viewModel.onSave()
            viewModel.onAmountChange("5000")
            viewModel.onCategoryChange(food.id)

            val state = expectMostRecentItem()
            assertFalse(state.form.amountError)
            assertFalse(state.form.categoryError)
            assertEquals(food, state.selectedCategory)
        }
    }

    @Test
    fun `valid form is saved as a manual transaction`() = runTest {
        coEvery { addTransaction(any(), any(), any(), any(), any(), any()) } returns AddTransactionResult.Success(1)
        val viewModel = createViewModel()
        viewModel.uiState.test {
            viewModel.onMerchantChange("Warung Bu Siti")
            viewModel.onAmountChange("35000")
            viewModel.onDateChange(today.minusDays(1))
            viewModel.onCategoryChange(food.id)
            viewModel.onNoteChange("Makan siang")
            viewModel.onSave()

            assertTrue(expectMostRecentItem().form.isSaved)
        }
        coVerify {
            addTransaction(35_000, today.minusDays(1), "Warung Bu Siti", "Makan siang", food.id, TransactionSource.MANUAL)
        }
    }

    @Test
    fun `date error from the use case is shown on the date field`() = runTest {
        coEvery { addTransaction(any(), any(), any(), any(), any(), any()) } returns
            AddTransactionResult.Invalid(TransactionError.DATE_IN_FUTURE)
        val viewModel = createViewModel()
        viewModel.uiState.test {
            viewModel.onAmountChange("10000")
            viewModel.onCategoryChange(food.id)
            viewModel.onSave()

            val form = expectMostRecentItem().form
            assertTrue(form.dateError)
            assertFalse(form.isSaved)
            assertFalse(form.isSaving)
        }
    }

    @Test
    fun `pressing save twice while saving only saves once`() = runTest {
        val pending = CompletableDeferred<AddTransactionResult>()
        coEvery { addTransaction(any(), any(), any(), any(), any(), any()) } coAnswers { pending.await() }
        val viewModel = createViewModel()
        viewModel.uiState.test {
            viewModel.onAmountChange("10000")
            viewModel.onCategoryChange(food.id)
            viewModel.onSave()
            viewModel.onSave()
            pending.complete(AddTransactionResult.Success(1))

            assertTrue(expectMostRecentItem().form.isSaved)
        }
        coVerify(exactly = 1) { addTransaction(any(), any(), any(), any(), any(), any()) }
    }
}
