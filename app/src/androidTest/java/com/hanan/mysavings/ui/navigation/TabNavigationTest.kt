package com.hanan.mysavings.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Bottom-bar behaviour without any UI: which tab is current, and what Back does. */
@RunWith(AndroidJUnit4::class)
class TabNavigationTest {

    @Test
    fun startsOnTheExpensesTab() = onMainThread {
        val navController = navControllerWithAppGraph()

        assertEquals(TopLevelTab.EXPENSES, currentTab(navController))
    }

    @Test
    fun backFromAnotherTabReturnsToExpenses() = onMainThread {
        val navController = navControllerWithAppGraph()

        navController.navigateToTab(TopLevelTab.SAVINGS)
        navController.navigateToTab(TopLevelTab.DEBTS)
        assertEquals(TopLevelTab.DEBTS, currentTab(navController))

        // Tabs replace each other instead of stacking, so one Back goes straight to the start tab.
        navController.popBackStack()
        assertEquals(TopLevelTab.EXPENSES, currentTab(navController))
        // Nothing is left behind it, so the next Back leaves the app.
        assertNull(navController.previousBackStackEntry)
    }

    @Test
    fun tappingTheCurrentTabAgainDoesNotStackIt() = onMainThread {
        val navController = navControllerWithAppGraph()

        navController.navigateToTab(TopLevelTab.PROFILE)
        navController.navigateToTab(TopLevelTab.PROFILE)
        navController.popBackStack()

        assertEquals(TopLevelTab.EXPENSES, currentTab(navController))
    }

    @Test
    fun detailScreensHaveNoTabSoTheBarIsHidden() = onMainThread {
        val navController = navControllerWithAppGraph()

        navController.navigate(Destination.Confirm())
        assertNull(currentTab(navController))

        navController.navigateToTab(TopLevelTab.PROFILE)
        navController.navigate(Destination.Settings)
        assertNull(currentTab(navController))
    }

    private fun currentTab(navController: NavHostController) = tabOf(navController.currentDestination!!)

    /** Same destinations as MySavingsNavHost, without any UI. */
    private fun navControllerWithAppGraph() = NavHostController(ApplicationProvider.getApplicationContext()).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        graph = createGraph(startDestination = Destination.Home) {
            composable<Destination.Home> {}
            composable<Destination.Savings> {}
            composable<Destination.Debts> {}
            composable<Destination.Profile> {}
            composable<Destination.Scan> {}
            composable<Destination.Stats> {}
            composable<Destination.Settings> {}
            composable<Destination.Confirm>(typeMap = Destination.Confirm.typeMap) {}
        }
    }

    private fun onMainThread(block: () -> Unit) {
        var failure: Throwable? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            try {
                block()
            } catch (t: Throwable) {
                failure = t
            }
        }
        failure?.let { throw it }
    }
}
