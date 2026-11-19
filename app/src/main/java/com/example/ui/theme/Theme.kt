package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.local.AppThemeMode

private val BetShieldDarkColorScheme =
  darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = DeepNavy,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = ProtectionGreenLight,
    onSecondary = DeepNavy,
    secondaryContainer = ProtectionGreenDarkContainer,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = WarningAmber,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkOutline,
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D),
  )

private val BetShieldLightColorScheme =
  lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = ProtectionGreen,
    onSecondary = Color.White,
    secondaryContainer = ProtectionGreenContainer,
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = WarningAmber,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextMuted,
    outline = LightOutline,
    error = AlertRed,
    errorContainer = AlertRedContainer,
  )

val BetShieldShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun BetShieldTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val darkTheme =
    when (themeMode) {
      AppThemeMode.SYSTEM -> isSystemInDarkTheme()
      AppThemeMode.LIGHT -> false
      AppThemeMode.DARK -> true
    }

  MaterialTheme(
    colorScheme = if (darkTheme) BetShieldDarkColorScheme else BetShieldLightColorScheme,
    typography = BetShieldTypography,
    shapes = BetShieldShapes,
    content = content,
  )
}
