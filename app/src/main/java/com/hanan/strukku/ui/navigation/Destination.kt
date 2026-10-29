package com.hanan.strukku.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations. Navigation Compose serializes these objects into
 * routes, so a typo or a missing argument is a compile error instead of a runtime crash.
 * Destinations that need arguments later (e.g. a scanned draft) become data classes.
 */
sealed interface Destination {
    @Serializable
    data object Home : Destination

    @Serializable
    data object Scan : Destination

    @Serializable
    data object Confirm : Destination
}
