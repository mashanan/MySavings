package com.hanan.strukku.ui.navigation

import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hanan.strukku.domain.model.ReceiptField
import com.hanan.strukku.ui.confirm.ConfirmPrefill
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks that the scanned draft really survives the trip through Navigation: the custom
 * NavType must encode it into the route and decode it on the other side, including text
 * that needs URL-encoding.
 */
@RunWith(AndroidJUnit4::class)
class ConfirmNavigationTest {

    private val prefill = ConfirmPrefill(
        merchant = "Kopi & Roti / Bu Siti 100%",
        total = 87_500,
        dateEpochDay = 20_000,
        categoryId = 3,
        needsCheck = setOf(ReceiptField.DATE, ReceiptField.CATEGORY),
        fromScan = true,
    )

    @Test
    fun scannedPrefillArrivesUnchanged() = onMainThread {
        val navController = navControllerWithAppGraph()

        navController.navigate(Destination.Confirm(prefill))

        assertEquals(prefill, navController.currentBackStackEntry!!.toRoute<Destination.Confirm>().prefill)
    }

    @Test
    fun confirmWithoutArgumentsIsManualEntry() = onMainThread {
        val navController = navControllerWithAppGraph()

        navController.navigate(Destination.Confirm())

        assertEquals(ConfirmPrefill.Manual, navController.currentBackStackEntry!!.toRoute<Destination.Confirm>().prefill)
    }

    @Test
    fun navTypeRoundTripsThroughRouteStringAndBundle() {
        val navType = serializableNavType<ConfirmPrefill>()

        assertEquals(prefill, navType.parseValue(navType.serializeAsValue(prefill)))

        val bundle = android.os.Bundle()
        navType.put(bundle, "prefill", prefill)
        assertEquals(prefill, navType[bundle, "prefill"])
    }

    /** Same destinations and type map as StrukKuNavHost, without any UI. */
    private fun navControllerWithAppGraph() = NavHostController(ApplicationProvider.getApplicationContext()).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        graph = createGraph(startDestination = Destination.Home) {
            composable<Destination.Home> {}
            composable<Destination.Scan> {}
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
