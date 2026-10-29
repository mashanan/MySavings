package com.hanan.mysavings.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    primaryContainer = GreenContainer90,
    onPrimaryContainer = OnGreenContainer10,
    secondary = Sage40,
    secondaryContainer = SageContainer90,
    tertiary = Amber40,
    tertiaryContainer = AmberContainer90,
)

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    primaryContainer = GreenContainer30,
    secondary = Sage80,
    secondaryContainer = SageContainer30,
    tertiary = Amber80,
    tertiaryContainer = AmberContainer30,
)

@Composable
fun MySavingsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the brand colors look the same on every device.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
