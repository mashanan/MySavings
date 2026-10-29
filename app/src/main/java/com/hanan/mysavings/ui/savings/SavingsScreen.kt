package com.hanan.mysavings.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.domain.model.GoalProgress
import com.hanan.mysavings.domain.model.SavingsGoal
import com.hanan.mysavings.domain.usecase.GoalWithProgress
import com.hanan.mysavings.ui.components.IconMessage
import com.hanan.mysavings.ui.components.MySavingsTopBar
import com.hanan.mysavings.ui.theme.MySavingsTheme
import com.hanan.mysavings.ui.util.currentLocale
import com.hanan.mysavings.ui.util.formatRupiah
import com.hanan.mysavings.ui.util.formatShortDate
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun SavingsRoute(
    onNewGoal: () -> Unit,
    onGoalClick: (Long) -> Unit,
    viewModel: SavingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SavingsScreen(
        uiState = uiState,
        onNewGoal = onNewGoal,
        onGoalClick = onGoalClick,
        onFilterChange = viewModel::onFilterChange,
    )
}

@Composable
fun SavingsScreen(
    uiState: SavingsUiState,
    onNewGoal: () -> Unit,
    onGoalClick: (Long) -> Unit,
    onFilterChange: (GoalCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { MySavingsTopBar(title = stringResource(R.string.tab_savings)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewGoal,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.savings_new_goal)) },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            SavingsUiState.Loading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }

            is SavingsUiState.Content -> if (uiState.goalCount == 0) {
                IconMessage(
                    icon = R.drawable.ic_tab_savings,
                    title = stringResource(R.string.savings_empty_title),
                    body = stringResource(R.string.savings_empty_body),
                    modifier = Modifier.padding(innerPadding),
                )
            } else {
                SavingsContent(uiState, onGoalClick, onFilterChange, innerPadding)
            }
        }
    }
}

@Composable
private fun SavingsContent(
    state: SavingsUiState.Content,
    onGoalClick: (Long) -> Unit,
    onFilterChange: (GoalCategory?) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        // Bottom padding keeps the last card clear of the FAB.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "total") {
            TotalSavingsCard(totalSaved = state.totalSaved, reachedCount = state.reachedCount, goalCount = state.goalCount)
        }
        // With a single category there is nothing to filter.
        if (state.categories.size > 1) {
            item(key = "filters") {
                CategoryFilterRow(categories = state.categories, selected = state.filter, onSelect = onFilterChange)
            }
        }
        items(state.goals, key = { it.goal.id }) {
            GoalCard(item = it, onClick = { onGoalClick(it.goal.id) })
        }
    }
}

@Composable
private fun TotalSavingsCard(totalSaved: Long, reachedCount: Int, goalCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.savings_total_title), style = MaterialTheme.typography.titleSmall)
            Text(
                text = formatRupiah(totalSaved),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.savings_reached_count, reachedCount, goalCount),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<GoalCategory>,
    selected: GoalCategory?,
    onSelect: (GoalCategory?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.savings_filter_all)) },
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(stringResource(category.labelRes())) },
                leadingIcon = { Icon(painterResource(category.iconRes()), contentDescription = null) },
            )
        }
    }
}

@Composable
private fun GoalCard(item: GoalWithProgress, onClick: () -> Unit) {
    val goal = item.goal
    val progress = item.progress
    val percent = (progress.fraction * 100).roundToInt()
    val progressDescription = stringResource(R.string.goal_progress_desc, percent)

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoalCategoryIcon(goal.category)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = goalStatusText(goal, progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (progress.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(text = "$percent%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { progress.fraction.coerceIn(0f, 1f) },
                color = goal.category.color(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .semantics { stateDescription = progressDescription },
            )
            Text(
                text = stringResource(R.string.goal_progress_amount, formatRupiah(progress.saved), formatRupiah(progress.target)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** One short line under the goal name: reached, overdue, deadline, or just the category. */
@Composable
internal fun goalStatusText(goal: SavingsGoal, progress: GoalProgress): String = when {
    progress.isReached -> stringResource(R.string.goal_reached)
    progress.isOverdue -> stringResource(R.string.goal_overdue)
    goal.targetDate != null -> stringResource(R.string.goal_deadline, formatShortDate(goal.targetDate, currentLocale()))
    else -> stringResource(goal.category.labelRes())
}

@Preview(showBackground = true)
@Composable
private fun SavingsScreenPreview() {
    val today = LocalDate.of(2026, 10, 29)
    fun goal(id: Long, name: String, category: GoalCategory, target: Long, saved: Long, deadline: LocalDate?) =
        SavingsGoal(id, name, target, category, deadline, Instant.EPOCH).let {
            GoalWithProgress(it, GoalProgress.of(it, saved, today))
        }
    val goals = listOf(
        goal(1, "Laptop kerja", GoalCategory.GADGET, 12_000_000, 4_500_000, LocalDate.of(2027, 3, 1)),
        goal(2, "Liburan ke Bali", GoalCategory.TRAVEL, 6_000_000, 1_200_000, null),
        goal(3, "Dana darurat", GoalCategory.EMERGENCY, 5_000_000, 5_000_000, null),
    )
    MySavingsTheme {
        SavingsScreen(
            uiState = SavingsUiState.Content(
                totalSaved = 10_700_000,
                reachedCount = 1,
                goalCount = 3,
                categories = goals.map { it.goal.category },
                filter = null,
                goals = goals,
            ),
            onNewGoal = {},
            onGoalClick = {},
            onFilterChange = {},
        )
    }
}
