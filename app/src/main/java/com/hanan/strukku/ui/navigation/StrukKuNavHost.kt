package com.hanan.strukku.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hanan.strukku.ui.confirm.ConfirmRoute
import com.hanan.strukku.ui.home.HomeRoute
import com.hanan.strukku.ui.scan.ScanScreen

/** SavedStateHandle key Confirm sets on Home's back stack entry after a successful save. */
private const val KEY_TRANSACTION_SAVED = "transaction_saved"

/**
 * The app's navigation graph. Screens never touch [NavHostController] directly; they
 * receive plain lambdas, which keeps them previewable and testable in isolation.
 */
@Composable
fun StrukKuNavHost(
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
                onManualEntryClick = { navController.navigate(Destination.Confirm) },
                showSavedMessage = transactionSaved,
                onSavedMessageShown = { entry.savedStateHandle[KEY_TRANSACTION_SAVED] = false },
            )
        }
        composable<Destination.Scan> {
            ScanScreen(
                onBack = { navController.popBackStack() },
                onReceiptCaptured = { navController.navigate(Destination.Confirm) },
            )
        }
        composable<Destination.Confirm> {
            ConfirmRoute(
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
