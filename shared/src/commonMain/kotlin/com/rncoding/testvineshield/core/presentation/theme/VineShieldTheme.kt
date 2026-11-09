package com.rncoding.testvineshield.core.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val light = lightColorScheme(
    primary = Color(0xFF365C30),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA8D08D),
    onPrimaryContainer = Color(0xFF333333),
    secondary = Color(0xFF66558D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB0A4D9),
    onSecondaryContainer = Color(0xFF333333),
    background = Color(0xFFF9F9F9),
    onBackground = Color(0xFF333333),
    surface = Color.White,
    onSurface = Color(0xFF333333),
    surfaceVariant = Color(0xFFE6ECDD),
    onSurfaceVariant = Color(0xFF565E52),
    outline = Color(0xFF74796F)
)

private val dark = darkColorScheme(
    primary = Color(0xFFA8D08D),
    onPrimary = Color(0xFF193214),
    primaryContainer = Color(0xFF2C4727),
    onPrimaryContainer = Color(0xFFD0E8BE),
    secondary = Color(0xFFD1C4EF),
    onSecondary = Color(0xFF34264F),
    secondaryContainer = Color(0xFF4D3E67),
    onSecondaryContainer = Color(0xFFEADCFF),
    background = Color(0xFF121812),
    onBackground = Color(0xFFE4E9E0),
    surface = Color(0xFF181F18),
    onSurface = Color(0xFFE4E9E0),
    surfaceVariant = Color(0xFF3E483A),
    onSurfaceVariant = Color(0xFFC3CCBC),
    outline = Color(0xFF8D9786)
)

@Composable
fun VineShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) dark else light,
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}
