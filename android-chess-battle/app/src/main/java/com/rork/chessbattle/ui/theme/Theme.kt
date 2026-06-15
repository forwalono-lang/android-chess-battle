package com.rork.chessbattle.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ChessDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB58863),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF8B6914),
    secondary = Color(0xFFF0D9B5),
    onSecondary = Color(0xFF1E1B18),
    tertiary = Color(0xFF829769),
    background = Color(0xFF1E1B18),
    onBackground = Color(0xFFF0D9B5),
    surface = Color(0xFF2A2724),
    onSurface = Color(0xFFE8E0D5),
    surfaceVariant = Color(0xFF3D3A37),
    onSurfaceVariant = Color(0xFFC4B9A8),
    outline = Color(0xFF5C5854)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> ChessDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
