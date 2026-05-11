package com.example.multipageapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.multipageapp.domain.AccentPalette
import com.example.multipageapp.domain.AppTheme
import com.example.multipageapp.domain.UserPreferences

@Composable
fun FocusFlowTheme(
    prefs: UserPreferences,
    content: @Composable () -> Unit
) {
    val effectiveDark = prefs.theme != AppTheme.LIGHT
    val (primary, tertiary) = when (prefs.accent) {
        AccentPalette.SLATE -> SlatePrimary to SlateSecondary
        AccentPalette.OCEAN -> OceanPrimary to OceanSecondary
        AccentPalette.LAVENDER -> LavenderPrimary to LavenderSecondary
        AccentPalette.AURORA -> AuroraPrimary to AuroraSecondary
    }
    val background = when {
        prefs.theme == AppTheme.AMOLED -> Color.Black
        effectiveDark -> Ink
        else -> Color(0xFFF8FAFC)
    }
    val surface = when {
        prefs.theme == AppTheme.AMOLED -> Color(0xFF0A0A0A)
        effectiveDark -> Midnight
        else -> Color.White
    }
    val onBg = if (effectiveDark) SoftWhite else Color(0xFF0F172A)
    val scheme = if (effectiveDark) {
        darkColorScheme(
            primary = primary,
            secondary = tertiary,
            tertiary = tertiary,
            background = background,
            surface = surface,
            onBackground = onBg,
            onSurface = onBg,
            onPrimary = Color.Black,
            surfaceVariant = surface.copy(alpha = 0.92f)
        )
    } else {
        lightColorScheme(
            primary = primary,
            secondary = tertiary,
            tertiary = tertiary,
            background = background,
            surface = surface,
            onBackground = onBg,
            onSurface = onBg,
            onPrimary = Color.Black
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = background.toArgb()
            window.navigationBarColor = background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !effectiveDark
                isAppearanceLightNavigationBars = !effectiveDark
            }
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
