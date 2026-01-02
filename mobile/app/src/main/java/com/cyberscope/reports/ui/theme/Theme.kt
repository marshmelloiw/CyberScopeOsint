package com.cyberscope.reports.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = CyberBlue80,
    onPrimary = Grey10,
    primaryContainer = CyberBlue20,
    onPrimaryContainer = Grey90,
    
    secondary = ElectricPurple80,
    onSecondary = Grey10,
    secondaryContainer = ElectricPurple20,
    onSecondaryContainer = Grey90,
    
    tertiary = NeonGreen80,
    onTertiary = Grey10,
    tertiaryContainer = NeonGreen20,
    onTertiaryContainer = Grey90,
    
    error = ErrorRed80,
    onError = Grey10,
    errorContainer = ErrorRed20,
    onErrorContainer = Grey90,
    
    background = Grey10,
    onBackground = Grey90,
    
    surface = Grey20,
    onSurface = Grey90,
    surfaceVariant = Grey30,
    onSurfaceVariant = Grey80,
    
    outline = Grey80
)

private val LightColorScheme = lightColorScheme(
    primary = CyberBlue40,
    onPrimary = Grey95,
    primaryContainer = CyberBlue80,
    onPrimaryContainer = Grey10,
    
    secondary = ElectricPurple40,
    onSecondary = Grey95,
    secondaryContainer = ElectricPurple80,
    onSecondaryContainer = Grey10,
    
    tertiary = NeonGreen40,
    onTertiary = Grey95,
    tertiaryContainer = NeonGreen80,
    onTertiaryContainer = Grey10,
    
    error = ErrorRed40,
    onError = Grey95,
    errorContainer = ErrorRed80,
    onErrorContainer = Grey10,
    
    background = Grey95,
    onBackground = Grey10,
    
    surface = Grey95,
    onSurface = Grey10,
    surfaceVariant = Grey90,
    onSurfaceVariant = Grey30,
    
    outline = Grey30
)

@Composable
fun CyberScopeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
