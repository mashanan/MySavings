package com.hanan.mysavings.ui.navigation

import com.hanan.mysavings.ui.confirm.ConfirmPrefill
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

/**
 * Type-safe navigation destinations. Navigation Compose serializes these objects into
 * routes, so a typo or a missing argument is a compile error instead of a runtime crash.
 */
sealed interface Destination {
    /** The "Expenses" tab. Kept as Home: it is still the start screen. */
    @Serializable
    data object Home : Destination

    @Serializable
    data object Savings : Destination

    /** Create a savings goal (`goalId == null`) or edit one. */
    @Serializable
    data class GoalForm(val goalId: Long? = null) : Destination

    @Serializable
    data class GoalDetail(val goalId: Long) : Destination

    @Serializable
    data object Debts : Destination

    /** Create a debt (`debtId == null`) or edit one. */
    @Serializable
    data class DebtForm(val debtId: Long? = null) : Destination

    @Serializable
    data class DebtDetail(val debtId: Long) : Destination

    @Serializable
    data object Profile : Destination

    @Serializable
    data object Scan : Destination

    @Serializable
    data object Stats : Destination

    @Serializable
    data object Settings : Destination

    /** Transaction form; [prefill] is empty for manual entry and holds the draft after a scan. */
    @Serializable
    data class Confirm(val prefill: ConfirmPrefill = ConfirmPrefill.Manual) : Destination {
        companion object {
            /** Tells Navigation how to store the custom [ConfirmPrefill] argument. */
            val typeMap = mapOf(typeOf<ConfirmPrefill>() to serializableNavType<ConfirmPrefill>())
        }
    }
}
