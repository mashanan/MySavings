package com.hanan.mysavings.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.Category

/** Category icon on a tinted circle in the category's own color. Decorative: the name is always shown next to it. */
@Composable
fun CategoryIcon(
    category: Category,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val color = categoryColor(category)
    Box(
        modifier = modifier
            .size(size)
            .background(color.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconFor(category.iconKey)),
            contentDescription = null,
            tint = color,
            modifier = Modifier.padding(size / 5),
        )
    }
}

fun categoryColor(category: Category): Color =
    runCatching { Color(category.colorHex.toColorInt()) }.getOrDefault(Color.Gray)

@DrawableRes
private fun iconFor(iconKey: String): Int = when (iconKey) {
    "food" -> R.drawable.ic_category_food
    "groceries" -> R.drawable.ic_category_groceries
    "transport" -> R.drawable.ic_category_transport
    "bills" -> R.drawable.ic_category_bills
    "entertainment" -> R.drawable.ic_category_entertainment
    "health" -> R.drawable.ic_category_health
    else -> R.drawable.ic_category_other
}
