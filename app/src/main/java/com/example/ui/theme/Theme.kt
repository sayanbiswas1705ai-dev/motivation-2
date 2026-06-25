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
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    secondary = BrandSecondaryDark,
    tertiary = BrandTertiaryDark,
    background = BrandBackgroundDark,
    surface = BrandSurfaceDark,
    surfaceVariant = BrandSurfaceVariantDark,
    onPrimary = Color(0xFF11161B),
    onSecondary = Color(0xFF11161B),
    onTertiary = Color(0xFF11161B),
    onBackground = BrandOnBackgroundDark,
    onSurface = BrandOnSurfaceDark,
    onSurfaceVariant = BrandOnSurfaceVariantDark,
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = BrandOnPrimaryContainerDark,
    outline = BrandOutlineDark,
    outlineVariant = BrandOutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    secondary = BrandSecondary,
    tertiary = BrandTertiary,
    background = BrandBackgroundLight,
    surface = BrandSurfaceLight,
    surfaceVariant = BrandSurfaceVariantLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = BrandOnBackgroundLight,
    onSurface = BrandOnSurfaceLight,
    onSurfaceVariant = BrandOnSurfaceVariantLight,
    primaryContainer = BrandPrimaryContainerLight,
    onPrimaryContainer = BrandOnPrimaryContainerLight,
    secondaryContainer = BrandSecondaryContainerLight,
    onSecondaryContainer = BrandOnSecondaryContainerLight,
    outline = BrandOutlineLight,
    outlineVariant = BrandOutlineVariantLight
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
