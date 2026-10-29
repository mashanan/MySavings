package com.hanan.strukku.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hanan.strukku.ui.confirm.ConfirmScreen
import com.hanan.strukku.ui.home.HomeScreen
import com.hanan.strukku.ui.scan.ScanScreen

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
        composable<Destination.Home> {
            HomeScreen(
                onScanClick = { navController.navigate(Destination.Scan) },
                onManualEntryClick = { navController.navigate(Destination.Confirm) },
            )
        }
        composable<Destination.Scan> {
            ScanScreen(
                onBack = { navController.popBackStack() },
                onReceiptCaptured = { navController.navigate(Destination.Confirm) },
            )
        }
        composable<Destination.Confirm> {
            ConfirmScreen(
                onBack = { navController.popBackStack() },
                // After saving, return to Home and drop Scan/Confirm from the back stack.
                onSaved = { navController.popBackStack(Destination.Home, inclusive = false) },
            )
        }
    }
}
