package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppTheme {
    OCEAN,
    VIOLET,
    FOREST,
    SUNSET,
    ROSE,
    SLATE;

    fun displayName(): String = when (this) {
        OCEAN -> "SHYLOCK"
        VIOLET -> "Violet"
        FOREST -> "Forest"
        SUNSET -> "Sunset"
        ROSE -> "Rose"
        SLATE -> "Slate"
    }
}

val ShylockGradientColors = listOf(
    Color(0xFF00F5D4),
    Color(0xFF00D9E8),
    Color(0xFF00BFFF),
    Color(0xFF00A8E8)
)

val ShylockLightColorScheme = lightColorScheme(
    primary = Color(0xFF00BFFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9FAFF),
    onPrimaryContainer = Color(0xFF00343D),
    secondary = Color(0xFF00C9A7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5FFF6),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = Color(0xFF00D9E8),
    onTertiary = Color(0xFF00363A),
    tertiaryContainer = Color(0xFFC8F9FC),
    onTertiaryContainer = Color(0xFF003438),
    background = Color(0xFFF7FBFC),
    onBackground = Color(0xFF0B1726),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B1726),
    surfaceVariant = Color(0xFFE8F5F7),
    onSurfaceVariant = Color(0xFF3F555C),
    outline = Color(0xFF7A969D),
    outlineVariant = Color(0xFFC5D9DD),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val ShylockDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF00343A),
    primaryContainer = Color(0xFF00515C),
    onPrimaryContainer = Color(0xFFB8F7FF),
    secondary = Color(0xFF00E0B8),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF005446),
    onSecondaryContainer = Color(0xFFA6F5E2),
    tertiary = Color(0xFF33E6F2),
    onTertiary = Color(0xFF003438),
    tertiaryContainer = Color(0xFF00545A),
    onTertiaryContainer = Color(0xFFB5F7FA),
    background = Color(0xFF061521),
    onBackground = Color(0xFFF4FCFF),
    surface = Color(0xFF0B1F2A),
    onSurface = Color(0xFFF4FCFF),
    surfaceVariant = Color(0xFF17313B),
    onSurfaceVariant = Color(0xFFB8D1D8),
    outline = Color(0xFF7899A2),
    outlineVariant = Color(0xFF314D56),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val ElegantDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF2B2930),
    onPrimaryContainer = Color(0xFFE6E1E5),
    secondary = Color(0xFFD0BCFF),
    onSecondary = Color(0xFF381E72),
    secondaryContainer = Color(0xFF49454F),
    onSecondaryContainer = Color(0xFFCAC4D0),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF2B2930),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF49454F)
)

private val ElegantLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
)

val LightSchemes = mapOf(
    AppTheme.OCEAN to ShylockLightColorScheme,
    AppTheme.VIOLET to ElegantLightColorScheme,
    AppTheme.FOREST to lightColorScheme(
        primary = Color(0xFF2E7D32),
        secondary = Color(0xFF6D8B3A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB0F2A4),
        onPrimaryContainer = Color(0xFF002204),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE6F4D0),
        onSecondaryContainer = Color(0xFF1D2800),
        background = Color(0xFFF7FBF2),
        onBackground = Color(0xFF191D17),
        surface = Color(0xFFF7FBF2),
        onSurface = Color(0xFF191D17),
        surfaceVariant = Color(0xFFDEE5D6),
        onSurfaceVariant = Color(0xFF42493F),
        outline = Color(0xFF72796E)
    ),
    AppTheme.SUNSET to lightColorScheme(
        primary = Color(0xFFE64A19),
        secondary = Color(0xFFF9A825),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDBD1),
        onPrimaryContainer = Color(0xFF3B0900),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFE9C2),
        onSecondaryContainer = Color(0xFF291B00),
        background = Color(0xFFFFF8F6),
        onBackground = Color(0xFF231A17),
        surface = Color(0xFFFFF8F6),
        onSurface = Color(0xFF231A17),
        surfaceVariant = Color(0xFFF5DED8),
        onSurfaceVariant = Color(0xFF53433F),
        outline = Color(0xFF85736E)
    ),
    AppTheme.ROSE to lightColorScheme(
        primary = Color(0xFFC2185B),
        secondary = Color(0xFF7B5EA7),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD9E2),
        onPrimaryContainer = Color(0xFF3F001C),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF2DAFF),
        onSecondaryContainer = Color(0xFF2C134E),
        background = Color(0xFFFFF8F8),
        onBackground = Color(0xFF201A1B),
        surface = Color(0xFFFFF8F8),
        onSurface = Color(0xFF201A1B),
        surfaceVariant = Color(0xFFF2DDE1),
        onSurfaceVariant = Color(0xFF514346),
        outline = Color(0xFF837376)
    ),
    AppTheme.SLATE to lightColorScheme(
        primary = Color(0xFF455A64),
        secondary = Color(0xFF78909C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCFE6F1),
        onPrimaryContainer = Color(0xFF001E2B),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE3F2FD),
        onSecondaryContainer = Color(0xFF071E2D),
        background = Color(0xFFF5F7F8),
        onBackground = Color(0xFF1C1E1F),
        surface = Color(0xFFF5F7F8),
        onSurface = Color(0xFF1C1E1F),
        surfaceVariant = Color(0xFFDFE3E6),
        onSurfaceVariant = Color(0xFF434749),
        outline = Color(0xFF73787A)
    )
)

val DarkSchemes = mapOf(
    AppTheme.OCEAN to ShylockDarkColorScheme,
    AppTheme.VIOLET to ElegantDarkColorScheme,
    AppTheme.FOREST to darkColorScheme(
        primary = Color(0xFF95D58C),
        secondary = Color(0xFFC6D799),
        onPrimary = Color(0xFF00390A),
        primaryContainer = Color(0xFF0E5217),
        onPrimaryContainer = Color(0xFFB0F2A4),
        onSecondary = Color(0xFF2B3A06),
        secondaryContainer = Color(0xFF41511B),
        onSecondaryContainer = Color(0xFFE6F4D0),
        background = Color(0xFF11140E),
        onBackground = Color(0xFFE1E3DC),
        surface = Color(0xFF11140E),
        onSurface = Color(0xFFE1E3DC),
        surfaceVariant = Color(0xFF42493F),
        onSurfaceVariant = Color(0xFFC2C8BC),
        outline = Color(0xFF8C9388)
    ),
    AppTheme.SUNSET to darkColorScheme(
        primary = Color(0xFFFFB5A0),
        secondary = Color(0xFFF9D18B),
        onPrimary = Color(0xFF5E1700),
        primaryContainer = Color(0xFF872505),
        onPrimaryContainer = Color(0xFFFFDBD1),
        onSecondary = Color(0xFF432E00),
        secondaryContainer = Color(0xFF654900),
        onSecondaryContainer = Color(0xFFFFE9C2),
        background = Color(0xFF1B120F),
        onBackground = Color(0xFFEDE0DC),
        surface = Color(0xFF1B120F),
        onSurface = Color(0xFFEDE0DC),
        surfaceVariant = Color(0xFF53433F),
        onSurfaceVariant = Color(0xFFD8C2BC),
        outline = Color(0xFFA08C87)
    ),
    AppTheme.ROSE to darkColorScheme(
        primary = Color(0xFFFFB1C8),
        secondary = Color(0xFFDEC2FF),
        onPrimary = Color(0xFF66002A),
        primaryContainer = Color(0xFF8F0042),
        onPrimaryContainer = Color(0xFFFFD9E2),
        onSecondary = Color(0xFF482971),
        secondaryContainer = Color(0xFF61438B),
        onSecondaryContainer = Color(0xFFF2DAFF),
        background = Color(0xFF1C1315),
        onBackground = Color(0xFFECE0E1),
        surface = Color(0xFF1C1315),
        onSurface = Color(0xFFECE0E1),
        surfaceVariant = Color(0xFF514346),
        onSurfaceVariant = Color(0xFFD5C2C5),
        outline = Color(0xFF9E8C90)
    ),
    AppTheme.SLATE to darkColorScheme(
        primary = Color(0xFFACCCD9),
        secondary = Color(0xFFB4C8D4),
        onPrimary = Color(0xFF18333D),
        primaryContainer = Color(0xFF2F4A56),
        onPrimaryContainer = Color(0xFFCFE6F1),
        onSecondary = Color(0xFF1F323B),
        secondaryContainer = Color(0xFF354852),
        onSecondaryContainer = Color(0xFFD0E4F2),
        background = Color(0xFF141617),
        onBackground = Color(0xFFE2E4E5),
        surface = Color(0xFF141617),
        onSurface = Color(0xFFE2E4E5),
        surfaceVariant = Color(0xFF434749),
        onSurfaceVariant = Color(0xFFC3C7C9),
        outline = Color(0xFF8D9193)
    )
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    appTheme: AppTheme = AppTheme.OCEAN,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkSchemes[appTheme] ?: ShylockDarkColorScheme
        else -> LightSchemes[appTheme] ?: ShylockLightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

