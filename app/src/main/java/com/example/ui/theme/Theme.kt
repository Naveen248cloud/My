package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = CoralPrimary,
    onPrimary = CoralOnPrimary,
    primaryContainer = Color(0xFF4B1020),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = IndigoSecondary,
    onSecondary = IndigoOnSecondary,
    secondaryContainer = Color(0xFF282266),
    onSecondaryContainer = Color(0xFFE1DEFF),
    tertiary = CyanTertiary,
    onTertiary = CyanOnTertiary,
    tertiaryContainer = Color(0xFF0A3954),
    onTertiaryContainer = Color(0xFFCFEFFF),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CoralPrimary,
    onPrimary = CoralOnPrimary,
    primaryContainer = CoralContainer,
    onPrimaryContainer = CoralOnContainer,
    secondary = IndigoSecondary,
    onSecondary = IndigoOnSecondary,
    secondaryContainer = IndigoContainer,
    onSecondaryContainer = IndigoOnContainer,
    tertiary = CyanTertiary,
    onTertiary = CyanOnTertiary,
    tertiaryContainer = CyanContainer,
    onTertiaryContainer = CyanOnContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
  )

private fun Color(hex: Long) = androidx.compose.ui.graphics.Color(hex)

val VibeShareShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK,
}

@Composable
fun VibeShareTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val isDark =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      isDark -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = VibeShareTypography,
    shapes = VibeShareShapes,
    content = content,
  )
}
