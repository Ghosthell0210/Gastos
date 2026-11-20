package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Palette - Emerald & Mineral Slate
val EmeraldPrimaryLight = Color(0xFF047857)
val OnEmeraldPrimaryLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFFD1FAE5)
val OnEmeraldContainerLight = Color(0xFF064E3B)

val SlateSecondaryLight = Color(0xFF334155)
val OnSlateSecondaryLight = Color(0xFFFFFFFF)
val SlateContainerLight = Color(0xFFE2E8F0)
val OnSlateContainerLight = Color(0xFF0F172A)

val TealTertiaryLight = Color(0xFF0F766E)
val OnTealTertiaryLight = Color(0xFFFFFFFF)
val TealContainerLight = Color(0xFFCCFBF1)
val OnTealContainerLight = Color(0xFF134E4A)

val BackgroundLight = Color(0xFFF4F7F6)
val OnBackgroundLight = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF0F172A)
val SurfaceVariantLight = Color(0xFFE6EFEA)
val OnSurfaceVariantLight = Color(0xFF475569)
val OutlineLight = Color(0xFFCBD5E1)
val OutlineVariantLight = Color(0xFFE2E8F0)

val ErrorLight = Color(0xFFDC2626)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF7F1D1D)

// Dark Palette - Deep Night Slate & Luminous Emerald
val EmeraldPrimaryDark = Color(0xFF34D399)
val OnEmeraldPrimaryDark = Color(0xFF022C22)
val EmeraldContainerDark = Color(0xFF064E3B)
val OnEmeraldContainerDark = Color(0xFFA7F3D0)

val SlateSecondaryDark = Color(0xFF94A3B8)
val OnSlateSecondaryDark = Color(0xFF0F172A)
val SlateContainerDark = Color(0xFF1E293B)
val OnSlateContainerDark = Color(0xFFE2E8F0)

val TealTertiaryDark = Color(0xFF5EEAD4)
val OnTealTertiaryDark = Color(0xFF042F2E)
val TealContainerDark = Color(0xFF115E59)
val OnTealContainerDark = Color(0xFFCCFBF1)

val BackgroundDark = Color(0xFF0B1215)
val OnBackgroundDark = Color(0xFFECFDF5)
val SurfaceDark = Color(0xFF131D22)
val OnSurfaceDark = Color(0xFFECFDF5)
val SurfaceVariantDark = Color(0xFF1A272E)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OutlineDark = Color(0xFF334155)
val OutlineVariantDark = Color(0xFF1E293B)

val ErrorDark = Color(0xFFF87171)
val OnErrorDark = Color(0xFF450A0A)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFECACA)

@Immutable
data class PesoClaroSemanticColors(
    val owedToMe: Color,
    val owedToMeContainer: Color,
    val iOwe: Color,
    val iOweContainer: Color,
    val budgetSafe: Color,
    val budgetWarning: Color,
    val budgetWarningContainer: Color,
    val budgetExceeded: Color,
    val budgetExceededContainer: Color,
    val heroGradientStart: Color,
    val heroGradientMid: Color,
    val heroGradientEnd: Color,
    val onHero: Color,
    val cardElevated: Color
)

val LightSemanticColors = PesoClaroSemanticColors(
    owedToMe = Color(0xFF059669),
    owedToMeContainer = Color(0xFFD1FAE5),
    iOwe = Color(0xFFD97706),
    iOweContainer = Color(0xFFFEF3C7),
    budgetSafe = Color(0xFF059669),
    budgetWarning = Color(0xFFD97706),
    budgetWarningContainer = Color(0xFFFEF3C7),
    budgetExceeded = Color(0xFFDC2626),
    budgetExceededContainer = Color(0xFFFEE2E2),
    heroGradientStart = Color(0xFF064E3B),
    heroGradientMid = Color(0xFF047857),
    heroGradientEnd = Color(0xFF0F172A),
    onHero = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFFFFFFF)
)

val DarkSemanticColors = PesoClaroSemanticColors(
    owedToMe = Color(0xFF34D399),
    owedToMeContainer = Color(0xFF064E3B),
    iOwe = Color(0xFFFBBF24),
    iOweContainer = Color(0xFF451A03),
    budgetSafe = Color(0xFF34D399),
    budgetWarning = Color(0xFFFBBF24),
    budgetWarningContainer = Color(0xFF451A03),
    budgetExceeded = Color(0xFFF87171),
    budgetExceededContainer = Color(0xFF7F1D1D),
    heroGradientStart = Color(0xFF065F46),
    heroGradientMid = Color(0xFF064E3B),
    heroGradientEnd = Color(0xFF0F172A),
    onHero = Color(0xFFECFDF5),
    cardElevated = Color(0xFF162228)
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }
