package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SophisticatedPrimary,
    secondary = SophisticatedSecondary,
    tertiary = SophisticatedTertiary,
    background = SophisticatedBg,
    surface = SophisticatedSurface,
    surfaceVariant = SophisticatedSurfaceVariant,
    onPrimary = SophisticatedOnPrimary,
    onSecondary = SophisticatedBg,
    onTertiary = SophisticatedBg,
    onBackground = SophisticatedOnBackground,
    onSurface = SophisticatedOnSurface,
    primaryContainer = SophisticatedPrimaryContainer,
    onPrimaryContainer = SophisticatedOnPrimaryContainer,
    outline = SophisticatedBorder,
    outlineVariant = SophisticatedBorder
)

private val LightColorScheme = lightColorScheme(
    primary = CohesiveLightPrimary,
    secondary = CohesiveLightSecondary,
    tertiary = CohesiveLightTertiary,
    background = CohesiveLightBg,
    surface = CohesiveLightSurface,
    surfaceVariant = CohesiveLightSurfaceVariant,
    onPrimary = CohesiveLightOnPrimary,
    onSecondary = CohesiveLightBg,
    onTertiary = CohesiveLightBg,
    onBackground = CohesiveLightOnBackground,
    onSurface = CohesiveLightOnSurface,
    primaryContainer = CohesiveLightPrimaryContainer,
    onPrimaryContainer = CohesiveLightOnPrimaryContainer,
    outline = CohesiveLightBorder,
    outlineVariant = CohesiveLightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to prioritize our beautiful customized brand colors
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
        content = content
    )
}
