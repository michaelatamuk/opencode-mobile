package com.opencode.mobile.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF0A0B0F)
private val SurfaceLow = Color(0xFF101319)
private val SurfaceMid = Color(0xFF161A22)
private val SurfaceHigh = Color(0xFF1D222C)
private val Accent = Color(0xFF8FB4FF)
private val AccentContainer = Color(0xFF22314F)
private val Mint = Color(0xFF9FE7C3)
private val MintContainer = Color(0xFF17342A)

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF06101F),
    primaryContainer = AccentContainer,
    onPrimaryContainer = Color(0xFFDCE7FF),
    secondary = Mint,
    onSecondary = Color(0xFF05160F),
    secondaryContainer = MintContainer,
    onSecondaryContainer = Color(0xFFC9F5DF),
    tertiary = Color(0xFFE7C6FF),
    onTertiary = Color(0xFF1B0A28),
    background = Ink,
    onBackground = Color(0xFFE8EAF0),
    surface = Ink,
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = SurfaceMid,
    onSurfaceVariant = Color(0xFF9BA3B4),
    surfaceContainerLowest = Color(0xFF070810),
    surfaceContainerLow = SurfaceLow,
    surfaceContainer = SurfaceLow,
    surfaceContainerHigh = SurfaceMid,
    surfaceContainerHighest = SurfaceHigh,
    outline = Color(0xFF2B313D),
    outlineVariant = Color(0xFF1B2029),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF3D0704),
    errorContainer = Color(0xFF4B1512),
    onErrorContainer = Color(0xFFFFDAD5),
    inverseSurface = Color(0xFFE8EAF0),
    inverseOnSurface = Color(0xFF191C22)
)

private val Paper = Color(0xFFF7F7F4)
private val LightScheme = lightColorScheme(
    primary = Color(0xFF2B47C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE3FF),
    onPrimaryContainer = Color(0xFF0B1B52),
    secondary = Color(0xFF1F6B4B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F0DF),
    onSecondaryContainer = Color(0xFF062517),
    background = Paper,
    onBackground = Color(0xFF14161B),
    surface = Color.White,
    onSurface = Color(0xFF14161B),
    surfaceVariant = Color(0xFFEDEEF2),
    onSurfaceVariant = Color(0xFF5B6070),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFBF9),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0F1F5),
    surfaceContainerHighest = Color(0xFFE7E9EF),
    outline = Color(0xFFC9CCD6),
    outlineVariant = Color(0xFFE3E5EC),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410E0B)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.9).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.5.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun OpenCodeTheme(
    mode: String = "dynamic",
    accent: String = "blue",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val amoled = mode == "amoled"
    val dark = darkTheme || amoled
    val base = when {
        mode == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        dark -> DarkScheme
        else -> LightScheme
    }
    val tinted = if (mode == "dynamic") base else base.accented(accent)
    val colorScheme = if (amoled) tinted.amoled() else tinted

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

private data class AccentColors(val primary: Color, val container: Color, val onContainer: Color)

private fun accentOf(name: String): AccentColors? = when (name) {
    "blue" -> AccentColors(Accent, AccentContainer, Color(0xFFDCE7FF))
    "mint" -> AccentColors(Mint, MintContainer, Color(0xFFC9F5DF))
    "violet" -> AccentColors(Color(0xFFE7C6FF), Color(0xFF2B1B3B), Color(0xFFF3E3FF))
    "amber" -> AccentColors(Color(0xFFFFD59B), Color(0xFF3A2A12), Color(0xFFFFEBC7))
    else -> null
}

private fun androidx.compose.material3.ColorScheme.accented(name: String): androidx.compose.material3.ColorScheme {
    val accent = accentOf(name) ?: return this
    return copy(
        primary = accent.primary,
        primaryContainer = accent.container,
        onPrimaryContainer = accent.onContainer
    )
}

/** True black surfaces for OLED panels, keeping the current accent. */
private fun androidx.compose.material3.ColorScheme.amoled(): androidx.compose.material3.ColorScheme =
    copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF070707),
        surfaceContainer = Color(0xFF0C0C0C),
        surfaceContainerHigh = Color(0xFF141414),
        surfaceContainerHighest = Color(0xFF1B1B1B)
    )
