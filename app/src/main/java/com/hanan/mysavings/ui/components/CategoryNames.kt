package com.hanan.mysavings.ui.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.Category

/**
 * Built-in categories are stored once in the database but must read in the user's language,
 * so their display name comes from string resources, keyed by the stable [Category.iconKey].
 * Categories the user creates keep the name they typed.
 */
@StringRes
fun defaultCategoryNameRes(iconKey: String): Int? = when (iconKey) {
    "food" -> R.string.category_food
    "groceries" -> R.string.category_groceries
    "transport" -> R.string.category_transport
    "bills" -> R.string.category_bills
    "entertainment" -> R.string.category_entertainment
    "health" -> R.string.category_health
    Category.OTHER_ICON_KEY -> R.string.category_other
    else -> null
}

/** Display name in the current UI language. */
@Composable
@ReadOnlyComposable
fun Category.displayName(): String =
    nameRes()?.let { stringResource(it) } ?: name

/** Display name for non-Compose code (PDF, notifications); pass a context in the app's language. */
fun Category.displayName(context: Context): String =
    nameRes()?.let(context::getString) ?: name

private fun Category.nameRes(): Int? = if (isDefault) defaultCategoryNameRes(iconKey) else null
