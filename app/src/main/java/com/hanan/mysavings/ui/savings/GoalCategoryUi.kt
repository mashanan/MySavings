package com.hanan.mysavings.ui.savings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.GoalCategory
import com.hanan.mysavings.ui.components.TintedCircleIcon

// How each goal category looks. Kept in the UI layer: the domain enum knows nothing about
// drawables, strings or colors.

@StringRes
fun GoalCategory.labelRes(): Int = when (this) {
    GoalCategory.GADGET -> R.string.goal_category_gadget
    GoalCategory.VEHICLE -> R.string.goal_category_vehicle
    GoalCategory.TRAVEL -> R.string.goal_category_travel
    GoalCategory.HOME -> R.string.goal_category_home
    GoalCategory.EDUCATION -> R.string.goal_category_education
    GoalCategory.EMERGENCY -> R.string.goal_category_emergency
    GoalCategory.OTHER -> R.string.goal_category_other
}

@DrawableRes
fun GoalCategory.iconRes(): Int = when (this) {
    GoalCategory.GADGET -> R.drawable.ic_goal_gadget
    GoalCategory.VEHICLE -> R.drawable.ic_category_transport
    GoalCategory.TRAVEL -> R.drawable.ic_goal_travel
    GoalCategory.HOME -> R.drawable.ic_goal_home
    GoalCategory.EDUCATION -> R.drawable.ic_goal_education
    GoalCategory.EMERGENCY -> R.drawable.ic_goal_emergency
    GoalCategory.OTHER -> R.drawable.ic_category_other
}

fun GoalCategory.color(): Color = when (this) {
    GoalCategory.GADGET -> Color(0xFF5C6BC0)
    GoalCategory.VEHICLE -> Color(0xFF42A5F5)
    GoalCategory.TRAVEL -> Color(0xFF26A69A)
    GoalCategory.HOME -> Color(0xFF8D6E63)
    GoalCategory.EDUCATION -> Color(0xFFFFA726)
    GoalCategory.EMERGENCY -> Color(0xFFEF5350)
    GoalCategory.OTHER -> Color(0xFF78909C)
}

/** Decorative: the category or goal name is always shown next to it. */
@Composable
fun GoalCategoryIcon(
    category: GoalCategory,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    TintedCircleIcon(icon = category.iconRes(), color = category.color(), modifier = modifier, size = size)
}
