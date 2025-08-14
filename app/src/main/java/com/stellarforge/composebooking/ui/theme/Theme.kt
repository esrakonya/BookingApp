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

// Koyu tema için özel renk şemamız
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryContainerBlue, // Karanlık temada ana renk daha açık ve soluk olabilir
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

// Açık tema için özel renk şemamız
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
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

@Composable
fun ComposeBookingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dinamik renk (Android 12+), duvar kağıdından renk almayı sağlar.
    // Şablonun markalaşması için bunu 'false' yapmak, her zaman kendi renklerinizi kullanmanızı sağlar.
    // 'true' bırakmak ise kullanıcıya daha kişisel bir deneyim sunar. Bu bir tasarım kararıdır.
    dynamicColor: Boolean = false, // Şimdilik false yapalım ki kendi renklerimizi görelim
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

    // Status bar (en üstteki saat, pil ikonu olan bar) rengini temaya uygun hale getirelim.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb() // Status bar rengini ana renge ayarla
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Type.kt'den gelen tipografi ölçeği
        // shapes = AppShapes, // Eğer özel şekillerin varsa (Shapes.kt)
        content = content
    )
}