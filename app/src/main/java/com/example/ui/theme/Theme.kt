package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = SafetyOrangeLight,
  onPrimary = Color(0xFF2B0E00),
  primaryContainer = Color(0xFF662700),
  onPrimaryContainer = SafetyOrangeContainer,
  secondary = CyanDiagnosticLight,
  onSecondary = Color(0xFF00263A),
  secondaryContainer = Color(0xFF00486B),
  onSecondaryContainer = CyanContainer,
  tertiary = EmeraldReady,
  background = NavySlateDark,
  onBackground = Color(0xFFF1F5F9),
  surface = NavySlateSurface,
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = NavySlateElevated,
  onSurfaceVariant = Color(0xFFCBD5E1),
  outline = Color(0xFF475569),
  error = CrimsonError,
)

private val LightColorScheme = lightColorScheme(
  primary = SafetyOrange,
  onPrimary = Color.White,
  primaryContainer = SafetyOrangeContainer,
  onPrimaryContainer = OnSafetyOrangeContainer,
  secondary = CyanDiagnostic,
  onSecondary = Color.White,
  secondaryContainer = CyanContainer,
  onSecondaryContainer = OnCyanContainer,
  tertiary = EmeraldReady,
  background = LightBackground,
  onBackground = SlateTextPrimary,
  surface = LightSurface,
  onSurface = SlateTextPrimary,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = SlateTextSecondary,
  outline = LightOutline,
  error = CrimsonError,
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}
