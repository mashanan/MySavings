package com.hanan.mysavings.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * The app shell: the navigation graph with the bottom bar around it. The bar only shows on
 * the four tab screens; detail screens (Scan, Confirm, Stats, Settings) use Back instead.
 */
@Composable
fun MySavingsApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.let(::tabOf)

    Scaffold(
        // Each screen has its own Scaffold that handles the status and navigation bars, so this
        // outer one only reserves room for the bottom bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { navController.navigateToTab(tab) },
                            // The label is always visible, so the icon needs no description of its own.
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        MySavingsNavHost(
            navController = navController,
            // Consuming the padding stops the inner Scaffolds from adding the navigation bar inset
            // a second time underneath the bottom bar.
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        )
    }
}

/** The tab [destination] belongs to, or `null` for a detail screen (where the bottom bar is hidden). */
internal fun tabOf(destination: NavDestination): TopLevelTab? =
    TopLevelTab.entries.firstOrNull { tab -> destination.hierarchy.any { it.hasRoute(tab.destination::class) } }

/**
 * Standard bottom-bar navigation: one back stack entry per tab (launchSingleTop), each tab
 * keeps its state when you leave it (saveState/restoreState), and Back from any tab returns
 * to the start tab before leaving the app (popUpTo the start destination).
 */
internal fun NavHostController.navigateToTab(tab: TopLevelTab) {
    navigate(tab.destination) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
