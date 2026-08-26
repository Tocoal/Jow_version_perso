package com.perso.jow.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JowGreen = Color(0xFF2E7D32)
private val JowGreenDark = Color(0xFF1B5E20)
private val JowOrange = Color(0xFFEF6C00)

private val LightColors = lightColorScheme(
    primary = JowGreen,
    secondary = JowOrange,
    tertiary = JowGreenDark
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    secondary = Color(0xFFFFB74D),
    tertiary = Color(0xFF66BB6A)
)

@Composable
fun JowTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (useDarkTheme) DarkColors else LightColors, content = content)
}
