package com.moneymanagement.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────
// Semantic transaction & status colors (Fintech 60/30/10 Rule)
// ──────────────────────────────────────────────────────────────
val IncomeGreen = Color(0xFF10B981)              // Cyber Emerald 500
val IncomeGreenContainer = Color(0xFFD1FAE5)     // Emerald 100
val ExpenseRed = Color(0xFFF43F5E)               // Rose 500
val ExpenseRedContainer = Color(0xFFFFE4E6)      // Rose 100
val TransferBlue = Color(0xFF3B82F6)             // Royal Blue 500
val TransferBlueContainer = Color(0xFFDBEAFE)    // Blue 100
val AmberWarning = Color(0xFFF59E0B)             // Amber 500
val SavingsGold = Color(0xFFF59E0B)              // Warm Gold
val EmergencyCyan = Color(0xFF06B6D4)            // Cyan 500 (Safety Shield)
val EmergencyCyanContainer = Color(0xFFCFFAFE)   // Cyan 100

// ──────────────────────────────────────────────────────────────
// Hero gradient color pairs — used on dashboard & account cards
// ──────────────────────────────────────────────────────────────
// Dark theme gradient (Obsidian Slate → Deep Indigo → Subtle Glow)
val DarkGradientStart = Color(0xFF0F172A)
val DarkGradientEnd = Color(0xFF1E1B4B)
val DarkGradientAccent = Color(0xFF312E81)

// Light theme gradient (Royal Indigo → Sleek Iris)
val LightGradientStart = Color(0xFF4338CA)
val LightGradientEnd = Color(0xFF6366F1)

// Account-type accent gradients
val AccountBankStart = Color(0xFF1D4ED8)
val AccountBankEnd = Color(0xFF3B82F6)
val AccountCashStart = Color(0xFF059669)
val AccountCashEnd = Color(0xFF10B981)
val AccountWalletStart = Color(0xFF7C3AED)
val AccountWalletEnd = Color(0xFFA855F7)
val AccountCreditStart = Color(0xFFBE123C)
val AccountCreditEnd = Color(0xFFF43F5E)

// ──────────────────────────────────────────────────────────────
// Chart palette — harmonious, high-contrast, dual-theme friendly
// ──────────────────────────────────────────────────────────────
val CHART_PALETTE = listOf(
    Color(0xFF6366F1), // Royal Indigo
    Color(0xFF06B6D4), // Cyan
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber Gold
    Color(0xFFF43F5E), // Rose
    Color(0xFF8B5CF6), // Purple
    Color(0xFF3B82F6), // Blue
    Color(0xFFEC4899), // Pink
    Color(0xFF14B8A6), // Teal
    Color(0xFF64748B), // Slate
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
    val emergencyCyan: Color = EmergencyCyan,
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
// Aura Money — Light Scheme (Clean, Breathable Porcelain Base)
// ──────────────────────────────────────────────────────────────
val md_theme_light_primary = Color(0xFF4F46E5)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFEEF2FF)
val md_theme_light_onPrimaryContainer = Color(0xFF1E1B4B)
val md_theme_light_secondary = Color(0xFF475569)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFF1F5F9)
val md_theme_light_onSecondaryContainer = Color(0xFF0F172A)
val md_theme_light_tertiary = Color(0xFF0D9488)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFCCFBF1)
val md_theme_light_onTertiaryContainer = Color(0xFF134E4A)
val md_theme_light_error = Color(0xFFE11D48)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFE4E6)
val md_theme_light_onErrorContainer = Color(0xFF881337)
val md_theme_light_background = Color(0xFFF8FAFC)
val md_theme_light_onBackground = Color(0xFF0F172A)
val md_theme_light_surface = Color(0xFFF8FAFC)
val md_theme_light_onSurface = Color(0xFF0F172A)
val md_theme_light_surfaceVariant = Color(0xFFE2E8F0)
val md_theme_light_onSurfaceVariant = Color(0xFF475569)
val md_theme_light_outline = Color(0xFF94A3B8)
val md_theme_light_outlineVariant = Color(0xFFCBD5E1)
val md_theme_light_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_theme_light_surfaceContainerLow = Color(0xFFF8FAFC)
val md_theme_light_surfaceContainer = Color(0xFFF1F5F9)
val md_theme_light_surfaceContainerHigh = Color(0xFFE2E8F0)
val md_theme_light_surfaceContainerHighest = Color(0xFFCBD5E1)
val md_theme_light_surfaceBright = Color(0xFFFFFFFF)
val md_theme_light_surfaceDim = Color(0xFFE2E8F0)
val md_theme_light_inverseSurface = Color(0xFF1E293B)
val md_theme_light_inverseOnSurface = Color(0xFFF8FAFC)
val md_theme_light_inversePrimary = Color(0xFF818CF8)

// ──────────────────────────────────────────────────────────────
// Aura Money — Dark Scheme (Rich Obsidian Void + High-End Contrast)
// ──────────────────────────────────────────────────────────────
val md_theme_dark_primary = Color(0xFF818CF8)
val md_theme_dark_onPrimary = Color(0xFF0F172A)
val md_theme_dark_primaryContainer = Color(0xFF312E81)
val md_theme_dark_onPrimaryContainer = Color(0xFFE0E7FF)
val md_theme_dark_secondary = Color(0xFF94A3B8)
val md_theme_dark_onSecondary = Color(0xFF0F172A)
val md_theme_dark_secondaryContainer = Color(0xFF1E293B)
val md_theme_dark_onSecondaryContainer = Color(0xFFE2E8F0)
val md_theme_dark_tertiary = Color(0xFF2DD4BF)
val md_theme_dark_onTertiary = Color(0xFF042F2E)
val md_theme_dark_tertiaryContainer = Color(0xFF115E59)
val md_theme_dark_onTertiaryContainer = Color(0xFFCCFBF1)
val md_theme_dark_error = Color(0xFFFB7185)
val md_theme_dark_onError = Color(0xFF4C0519)
val md_theme_dark_errorContainer = Color(0xFF9F1239)
val md_theme_dark_onErrorContainer = Color(0xFFFFE4E6)
val md_theme_dark_background = Color(0xFF090D16)
val md_theme_dark_onBackground = Color(0xFFF1F5F9)
val md_theme_dark_surface = Color(0xFF090D16)
val md_theme_dark_onSurface = Color(0xFFF1F5F9)
val md_theme_dark_surfaceVariant = Color(0xFF1E293B)
val md_theme_dark_onSurfaceVariant = Color(0xFF94A3B8)
val md_theme_dark_outline = Color(0xFF475569)
val md_theme_dark_outlineVariant = Color(0xFF334155)
val md_theme_dark_surfaceContainerLowest = Color(0xFF05080F)
val md_theme_dark_surfaceContainerLow = Color(0xFF0E1422)
val md_theme_dark_surfaceContainer = Color(0xFF131B2E)
val md_theme_dark_surfaceContainerHigh = Color(0xFF19243C)
val md_theme_dark_surfaceContainerHighest = Color(0xFF202D4A)
val md_theme_dark_surfaceBright = Color(0xFF283654)
val md_theme_dark_surfaceDim = Color(0xFF090D16)
val md_theme_dark_inverseSurface = Color(0xFFF1F5F9)
val md_theme_dark_inverseOnSurface = Color(0xFF0F172A)
val md_theme_dark_inversePrimary = Color(0xFF4F46E5)
