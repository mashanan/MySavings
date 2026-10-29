package com.hanan.mysavings.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanan.mysavings.R

/**
 * Up to two capital letters from the first two words of [name]: "Louise Hanan" -> "LH",
 * "budi" -> "B". `null` for a missing or blank name, so the caller can show an icon instead.
 */
fun initialsOf(name: String?): String? =
    name?.trim()
        ?.split(Regex("\\s+"))
        ?.filter { it.isNotEmpty() }
        ?.take(2)
        ?.joinToString("") { it.first().uppercase() }
        ?.ifEmpty { null }

/** The photo in a circle; without one, the initials, and without a name, a person icon. */
@Composable
fun ProfileAvatar(
    photo: ImageBitmap?,
    initials: String?,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(colors.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        when {
            photo != null -> Image(
                bitmap = photo,
                contentDescription = stringResource(R.string.profile_photo_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
            // Decorative: the name is shown right next to the avatar.
            initials != null -> Text(
                text = initials,
                fontSize = (size.value * 0.36f).sp,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimaryContainer,
            )
            else -> Icon(
                painter = painterResource(R.drawable.ic_tab_profile),
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}
