package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AgencyColorScheme = lightColorScheme(
    primary = Color(0xFF1E40AF), // Deep Indigo Blue
    secondary = Color(0xFF0369A1), // Sky Blue
    tertiary = Color(0xFF0F766E), // Classy Teal
    background = Color(0xFFF1F5F9), // Soft Slate Background
    surface = Color(0xFFFFFFFF), // Pure White Card
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A), // Dark Slate
    onSurface = Color(0xFF1E293B), // Medium-Dark Slate
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155)
)

private val StartupColorScheme = darkColorScheme(
    primary = Color(0xFF10B981), // Neon Mint Green
    secondary = Color(0xFF8B5CF6), // Neon Violet
    tertiary = Color(0xFFF59E0B), // Warm Glowing Amber
    background = Color(0xFF0B0F19), // Midnight Space Background
    surface = Color(0xFF161E2E), // Deep Moody Blue-Gray Surface
    onPrimary = Color(0xFF064E3B),
    onSecondary = Color(0xFF2E1065),
    onTertiary = Color(0xFF451A03),
    onBackground = Color(0xFFF3F4F6), // Cool Off-White
    onSurface = Color(0xFFE5E7EB), // Silver Gray
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF9CA3AF)
)

private val GrayColorScheme = darkColorScheme(
    primary = Color(0xFFE2E8F0), // Cool White / Silver Primary
    secondary = Color(0xFFCBD5E1), // Light Slate Gray Accent
    tertiary = Color(0xFF94A3B8), // Medium Slate Gray Accent
    background = Color(0xFF262626), // Deep Solid Gray Screen
    surface = Color(0xFF333333), // Medium Charcoal Gray Surface/Card
    onPrimary = Color(0xFF171717),
    onSecondary = Color(0xFF171717),
    onTertiary = Color(0xFF171717),
    onBackground = Color(0xFFFFFFFF), // Pure White Text on Gray Screen
    onSurface = Color(0xFFFFFFFF), // Pure White Text on Surface
    surfaceVariant = Color(0xFF404040),
    onSurfaceVariant = Color(0xFFF5F5F5)
)

private val DarkColorScheme =
  darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF252429),
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFF332D41),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFF7F2FA),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
  )

@Composable
fun MyApplicationTheme(
  dashboardStyle: String = "dark_moody",
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when (dashboardStyle) {
    "professional" -> AgencyColorScheme
    "dark_moody" -> StartupColorScheme
    "gray_minimal" -> GrayColorScheme
    else -> {
      if (darkTheme) DarkColorScheme else LightColorScheme
    }
  }

  val typography = when (dashboardStyle) {
    "professional" -> AgencyTypography
    "dark_moody" -> StartupTypography
    "gray_minimal" -> GrayTypography
    else -> Typography
  }

  MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}
