package com.moneymanagement.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────
// Semantic transaction colors — theme-independent
// ──────────────────────────────────────────────────────────────
val IncomeGreen = Color(0xFF00C853)
val IncomeGreenContainer = Color(0xFFB9F6CA)
val ExpenseRed = Color(0xFFFF5252)
val ExpenseRedContainer = Color(0xFFFFDAD9)
val TransferBlue = Color(0xFF448AFF)
val TransferBlueContainer = Color(0xFFD5E4FF)
val AmberWarning = Color(0xFFFFA726)
val SavingsGold = Color(0xFFFFB830)

// ──────────────────────────────────────────────────────────────
// Hero gradient color pairs — used on dashboard & account cards
// ──────────────────────────────────────────────────────────────
// Dark theme gradient (rich deep indigo → vivid violet)
val DarkGradientStart = Color(0xFF1A1A3E)
val DarkGradientEnd = Color(0xFF3D2E7C)
val DarkGradientAccent = Color(0xFF7B5EA7)

// Light theme gradient (soft indigo → periwinkle)
val LightGradientStart = Color(0xFF4A55A2)
val LightGradientEnd = Color(0xFF7F6DC8)

// Account-type accent gradients
val AccountBankStart = Color(0xFF1565C0)
val AccountBankEnd = Color(0xFF42A5F5)
val AccountCashStart = Color(0xFF2E7D32)
val AccountCashEnd = Color(0xFF66BB6A)
val AccountWalletStart = Color(0xFF6A1B9A)
val AccountWalletEnd = Color(0xFFCE93D8)
val AccountCreditStart = Color(0xFFC62828)
val AccountCreditEnd = Color(0xFFEF9A9A)

// ──────────────────────────────────────────────────────────────
// Chart palette — harmonious, high-contrast, dual-theme friendly
// ──────────────────────────────────────────────────────────────
val CHART_PALETTE = listOf(
    Color(0xFF7B5EA7), // violet
    Color(0xFF00BFA5), // teal
    Color(0xFFFF7043), // deep orange
    Color(0xFF42A5F5), // sky blue
    Color(0xFFFFCA28), // amber
    Color(0xFFEC407A), // pink
    Color(0xFF66BB6A), // green
    Color(0xFF26C6DA), // cyan
    Color(0xFFAB47BC), // purple
    Color(0xFF8D6E63), // brown
)

// ──────────────────────────────────────────────────────────────
// Custom app colors not in Material 3 — accessed via LocalAppColors
// ──────────────────────────────────────────────────────────────
@Immutable
data class AppColors(
    val gradientStart: Color,
    val gradientEnd: Color,
    val gradientAccent: Color,
    val incomeGreen: Color = IncomeGreen,
    val expenseRed: Color = ExpenseRed,
    val transferBlue: Color = TransferBlue,
    val savingsGold: Color = SavingsGold,
)

val LightAppColors = AppColors(
    gradientStart = LightGradientStart,
    gradientEnd = LightGradientEnd,
    gradientAccent = LightGradientEnd,
)

val DarkAppColors = AppColors(
    gradientStart = DarkGradientStart,
    gradientEnd = DarkGradientEnd,
    gradientAccent = DarkGradientAccent,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

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
val md_theme_light_background = Color(0xFFF6F5FF)
val md_theme_light_onBackground = Color(0xFF1B1B21)
val md_theme_light_surface = Color(0xFFF6F5FF)
val md_theme_light_onSurface = Color(0xFF1B1B21)
val md_theme_light_surfaceVariant = Color(0xFFE3E1EC)
val md_theme_light_onSurfaceVariant = Color(0xFF46464F)
val md_theme_light_outline = Color(0xFF777680)
val md_theme_light_outlineVariant = Color(0xFFC7C5D0)
val md_theme_light_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_theme_light_surfaceContainerLow = Color(0xFFF0EEFF)
val md_theme_light_surfaceContainer = Color(0xFFEAE8F5)
val md_theme_light_surfaceContainerHigh = Color(0xFFE4E2EF)
val md_theme_light_surfaceContainerHighest = Color(0xFFDEDCE9)
val md_theme_light_surfaceBright = Color(0xFFF6F5FF)
val md_theme_light_surfaceDim = Color(0xFFD8D7E3)
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
val md_theme_dark_background = Color(0xFF0F0F1A)
val md_theme_dark_onBackground = Color(0xFFE4E1EF)
val md_theme_dark_surface = Color(0xFF0F0F1A)
val md_theme_dark_onSurface = Color(0xFFE4E1EF)
val md_theme_dark_surfaceVariant = Color(0xFF46464F)
val md_theme_dark_onSurfaceVariant = Color(0xFFC7C5D0)
val md_theme_dark_outline = Color(0xFF91909A)
val md_theme_dark_outlineVariant = Color(0xFF46464F)
val md_theme_dark_surfaceContainerLowest = Color(0xFF09091A)
val md_theme_dark_surfaceContainerLow = Color(0xFF181826)
val md_theme_dark_surfaceContainer = Color(0xFF1C1C2E)
val md_theme_dark_surfaceContainerHigh = Color(0xFF252538)
val md_theme_dark_surfaceContainerHighest = Color(0xFF2E2E42)
val md_theme_dark_surfaceBright = Color(0xFF383852)
val md_theme_dark_surfaceDim = Color(0xFF0F0F1A)
val md_theme_dark_inverseSurface = Color(0xFFE4E1EF)
val md_theme_dark_inverseOnSurface = Color(0xFF303036)
val md_theme_dark_inversePrimary = Color(0xFF4A55A2)
