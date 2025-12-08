package com.stellarforge.composebooking.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Material 3 Color Scheme for Dark Mode.
 *
 * Design Choice: In Dark Mode, 'Primary' is lighter to ensure contrast against dark backgrounds.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryContainerBlue, // Light Blue-Grey
    onPrimary = OnPrimaryContainerBlue,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = OnPrimaryBlue,
    secondary = SecondaryContainerGrey,
    onSecondary = OnSecondaryContainerGrey,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = ErrorRed,
    onError = OnErrorRed,
    outline = OutlineDark
)

/**
 * Material 3 Color Scheme for Light Mode.
 *
 * Design Choice: In Light Mode, 'Primary' is the deep brand color (Navy Blue).
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue, // Deep Navy
    onPrimary = OnPrimaryBlue,
    primaryContainer = PrimaryContainerBlue,
    onPrimaryContainer = OnPrimaryContainerBlue,
    secondary = SecondaryGrey,
    onSecondary = OnSecondaryGrey,
    secondaryContainer = SecondaryContainerGrey,
    onSecondaryContainer = OnSecondaryContainerGrey,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = ErrorRed,
    onError = OnErrorRed,
    outline = OutlineLight
)

/**
 * Main Theme Composable for the Application.
 *
 * @param darkTheme Automatically detects system dark mode setting.
 * @param dynamicColor If true, picks colors from the user's wallpaper (Android 12+).
 *                     Defaults to `false` to enforce the Brand Identity (White-Label requirement).
 */
@Composable
fun ComposeBookingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep false to maintain consistent Brand Identity
    content: @Composable () -> Unit
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
            val window = (view.context as Activity).window

            // Set the Status Bar color to match the Primary Brand color
            window.statusBarColor = colorScheme.primary.toArgb()

            // --- STATUS BAR ICON LOGIC ---
            // Because our Light Theme uses a DARK Primary Color (Navy Blue),
            // we need Light Icons (isAppearanceLightStatusBars = false) in Light Mode.
            //
            // In Dark Theme, our Primary is Lighter, so we need Dark Icons (true).
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Ensure Type.kt exists
        content = content
    )
}