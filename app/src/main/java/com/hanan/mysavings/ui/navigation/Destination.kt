package com.hanan.mysavings.ui.navigation

import com.hanan.mysavings.ui.confirm.ConfirmPrefill
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

/**
 * Type-safe navigation destinations. Navigation Compose serializes these objects into
 * routes, so a typo or a missing argument is a compile error instead of a runtime crash.
 */
sealed interface Destination {
    @Serializable
    data object Home : Destination

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
