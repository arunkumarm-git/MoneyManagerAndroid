package com.moneymanagement.app.ui.theme

import androidx.compose.ui.graphics.Color

// Semantic colors used across screens for income/expense/transfer, independent of theme mode.
val IncomeGreen = Color(0xFF00C853)
val IncomeGreenContainer = Color(0xFFCCFADD)
val ExpenseRed = Color(0xFFEF5350)
val ExpenseRedContainer = Color(0xFFFCDBDA)
val TransferBlue = Color(0xFF5C6BC0)
val TransferBlueContainer = Color(0xFFDEE0F8)
val AmberWarning = Color(0xFFFFA726)

// Palette used to color category monogram avatars and chart series, cycled by index.
val CHART_PALETTE = listOf(
    Color(0xFF5C6BC0), Color(0xFFEF5350), Color(0xFF26A69A),
    Color(0xFFFFA726), Color(0xFFAB47BC), Color(0xFF29B6F6),
    Color(0xFFEC407A), Color(0xFF66BB6A), Color(0xFF8D6E63),
    Color(0xFF78909C),
)

// ──────────────────────────────────────────────────────────────
// Deep Indigo / Violet — light scheme
// ──────────────────────────────────────────────────────────────
val md_theme_light_primary = Color(0xFF4A55A2)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFDEE0FF)
val md_theme_light_onPrimaryContainer = Color(0xFF00105C)
val md_theme_light_secondary = Color(0xFF5B5D72)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFE0E0F9)
val md_theme_light_onSecondaryContainer = Color(0xFF181A2C)
val md_theme_light_tertiary = Color(0xFF7B5263)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFFFD8E7)
val md_theme_light_onTertiaryContainer = Color(0xFF301120)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFFBF8FF)
val md_theme_light_onBackground = Color(0xFF1B1B21)
val md_theme_light_surface = Color(0xFFFBF8FF)
val md_theme_light_onSurface = Color(0xFF1B1B21)
val md_theme_light_surfaceVariant = Color(0xFFE3E1EC)
val md_theme_light_onSurfaceVariant = Color(0xFF46464F)
val md_theme_light_outline = Color(0xFF777680)
val md_theme_light_outlineVariant = Color(0xFFC7C5D0)
val md_theme_light_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_theme_light_surfaceContainerLow = Color(0xFFF5F2FA)
val md_theme_light_surfaceContainer = Color(0xFFEFECF4)
val md_theme_light_surfaceContainerHigh = Color(0xFFEAE7EF)
val md_theme_light_surfaceContainerHighest = Color(0xFFE4E1E9)
val md_theme_light_surfaceBright = Color(0xFFFBF8FF)
val md_theme_light_surfaceDim = Color(0xFFDBD9E0)
val md_theme_light_inverseSurface = Color(0xFF303036)
val md_theme_light_inverseOnSurface = Color(0xFFF3F0F7)
val md_theme_light_inversePrimary = Color(0xFFBBC3FF)

// ──────────────────────────────────────────────────────────────
// Deep Indigo / Violet — dark scheme (warm-tinted dark surfaces)
// ──────────────────────────────────────────────────────────────
val md_theme_dark_primary = Color(0xFFBBC3FF)
val md_theme_dark_onPrimary = Color(0xFF1B2678)
val md_theme_dark_primaryContainer = Color(0xFF333D89)
val md_theme_dark_onPrimaryContainer = Color(0xFFDEE0FF)
val md_theme_dark_secondary = Color(0xFFC4C4DD)
val md_theme_dark_onSecondary = Color(0xFF2D2F42)
val md_theme_dark_secondaryContainer = Color(0xFF434559)
val md_theme_dark_onSecondaryContainer = Color(0xFFE0E0F9)
val md_theme_dark_tertiary = Color(0xFFEDB8CE)
val md_theme_dark_onTertiary = Color(0xFF482535)
val md_theme_dark_tertiaryContainer = Color(0xFF613B4B)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFD8E7)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF131318)
val md_theme_dark_onBackground = Color(0xFFE4E1E9)
val md_theme_dark_surface = Color(0xFF131318)
val md_theme_dark_onSurface = Color(0xFFE4E1E9)
val md_theme_dark_surfaceVariant = Color(0xFF46464F)
val md_theme_dark_onSurfaceVariant = Color(0xFFC7C5D0)
val md_theme_dark_outline = Color(0xFF91909A)
val md_theme_dark_outlineVariant = Color(0xFF46464F)
val md_theme_dark_surfaceContainerLowest = Color(0xFF0E0E13)
val md_theme_dark_surfaceContainerLow = Color(0xFF1B1B21)
val md_theme_dark_surfaceContainer = Color(0xFF1F1F25)
val md_theme_dark_surfaceContainerHigh = Color(0xFF2A2930)
val md_theme_dark_surfaceContainerHighest = Color(0xFF35343B)
val md_theme_dark_surfaceBright = Color(0xFF39393F)
val md_theme_dark_surfaceDim = Color(0xFF131318)
val md_theme_dark_inverseSurface = Color(0xFFE4E1E9)
val md_theme_dark_inverseOnSurface = Color(0xFF303036)
val md_theme_dark_inversePrimary = Color(0xFF4A55A2)
