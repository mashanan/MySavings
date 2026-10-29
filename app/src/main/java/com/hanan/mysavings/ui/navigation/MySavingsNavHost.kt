package com.hanan.mysavings.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hanan.mysavings.ui.confirm.ConfirmPrefill
import com.hanan.mysavings.ui.confirm.ConfirmRoute
import com.hanan.mysavings.ui.debts.DebtDetailRoute
import com.hanan.mysavings.ui.debts.DebtFormRoute
import com.hanan.mysavings.ui.debts.DebtsRoute
import com.hanan.mysavings.ui.home.HomeRoute
import com.hanan.mysavings.ui.profile.EditProfileRoute
import com.hanan.mysavings.ui.profile.ProfileRoute
import com.hanan.mysavings.ui.savings.GoalDetailRoute
import com.hanan.mysavings.ui.savings.GoalFormRoute
import com.hanan.mysavings.ui.savings.SavingsRoute
import com.hanan.mysavings.ui.scan.ScanRoute
import com.hanan.mysavings.ui.settings.SettingsRoute
import com.hanan.mysavings.ui.stats.StatsRoute

/** SavedStateHandle key Confirm sets on Home's back stack entry after a successful save. */
private const val KEY_TRANSACTION_SAVED = "transaction_saved"

/**
 * The app's navigation graph. Screens never touch [NavHostController] directly; they
 * receive plain lambdas, which keeps them previewable and testable in isolation.
 * Switching tabs is handled by the bottom bar in [MySavingsApp].
 */
@Composable
fun MySavingsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home,
        modifier = modifier,
    ) {
        composable<Destination.Home> { entry ->
            // Result passed back from Confirm. SavedStateHandle survives rotation and process death.
            val transactionSaved by entry.savedStateHandle
                .getStateFlow(KEY_TRANSACTION_SAVED, false)
                .collectAsStateWithLifecycle()
            HomeRoute(
                onScanClick = { navController.navigate(Destination.Scan) },
                onManualEntryClick = { navController.navigate(Destination.Confirm()) },
                onStatsClick = { navController.navigate(Destination.Stats) },
                showSavedMessage = transactionSaved,
                onSavedMessageShown = { entry.savedStateHandle[KEY_TRANSACTION_SAVED] = false },
            )
        }
        composable<Destination.Savings> {
            SavingsRoute(
                onNewGoal = { navController.navigate(Destination.GoalForm()) },
                onGoalClick = { navController.navigate(Destination.GoalDetail(it)) },
            )
        }
        composable<Destination.GoalForm> { entry ->
            GoalFormRoute(
                goalId = entry.toRoute<Destination.GoalForm>().goalId,
                onBack = { navController.popBackStack() },
                // Back to wherever the form was opened from: the list for a new goal, the detail for an edit.
                onSaved = { navController.popBackStack() },
            )
        }
        composable<Destination.GoalDetail> { entry ->
            val goalId = entry.toRoute<Destination.GoalDetail>().goalId
            GoalDetailRoute(
                goalId = goalId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Destination.GoalForm(goalId)) },
            )
        }
        composable<Destination.Debts> {
            DebtsRoute(
                onNewDebt = { navController.navigate(Destination.DebtForm()) },
                onDebtClick = { navController.navigate(Destination.DebtDetail(it)) },
            )
        }
        composable<Destination.DebtForm> { entry ->
            DebtFormRoute(
                debtId = entry.toRoute<Destination.DebtForm>().debtId,
                onBack = { navController.popBackStack() },
                // Back to wherever the form was opened from: the list for a new debt, the detail for an edit.
                onSaved = { navController.popBackStack() },
            )
        }
        composable<Destination.DebtDetail> { entry ->
            val debtId = entry.toRoute<Destination.DebtDetail>().debtId
            DebtDetailRoute(
                debtId = debtId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Destination.DebtForm(debtId)) },
            )
        }
        composable<Destination.Profile> {
            ProfileRoute(
                onEditProfile = { navController.navigate(Destination.EditProfile) },
                onSettingsClick = { navController.navigate(Destination.Settings) },
            )
        }
        composable<Destination.EditProfile> {
            EditProfileRoute(onBack = { navController.popBackStack() })
        }
        composable<Destination.Scan> {
            ScanRoute(
                onBack = { navController.popBackStack() },
                // Scan stays on the back stack, so Back from the form returns to the scan result.
                onContinue = { draft -> navController.navigate(Destination.Confirm(ConfirmPrefill.fromDraft(draft))) },
                // Typing it in replaces the scan, so Back from the form returns to Home.
                onManualEntry = {
                    navController.navigate(Destination.Confirm()) {
                        popUpTo<Destination.Scan> { inclusive = true }
                    }
                },
            )
        }
        composable<Destination.Stats> {
            StatsRoute(onBack = { navController.popBackStack() })
        }
        composable<Destination.Settings> {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
        composable<Destination.Confirm>(typeMap = Destination.Confirm.typeMap) { entry ->
            val route = entry.toRoute<Destination.Confirm>()
            ConfirmRoute(
                prefill = route.prefill,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.getBackStackEntry<Destination.Home>()
                        .savedStateHandle[KEY_TRANSACTION_SAVED] = true
                    // Return to Home and drop Scan/Confirm from the back stack.
                    navController.popBackStack(Destination.Home, inclusive = false)
                },
            )
        }
    }
}
