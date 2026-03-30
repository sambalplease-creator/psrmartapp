package com.psrmart.app.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp

// ─────────────────────────────────────────────
// FONTS — system defaults (no cert/network needed)
// ─────────────────────────────────────────────

val InterFamily    = FontFamily.SansSerif   // maps to Roboto/system sans on Android
val PlayfairFamily = FontFamily.Serif       // maps to serif for display/headings
val MontserratFamily = FontFamily.SansSerif

// ─────────────────────────────────────────────
// COLOUR PALETTE
// ─────────────────────────────────────────────

object PSRColors {
    // ── Forest greens (replaces navies) ───────────────────────────────────
    val Navy900   = Color(0xFF071A0E)   // deepest forest
    val Navy800   = Color(0xFF0C2414)
    val Navy700   = Color(0xFF113319)
    val Navy600   = Color(0xFF1A4D2E)   // primary brand green  ← most-used
    val Navy500   = Color(0xFF1F5C37)
    val Navy400   = Color(0xFF2E7D4F)
    val Navy300   = Color(0xFF3EA06A)
    val Accent    = Color(0xFF4CAF7D)   // fresh mid-green (was blue)
    val AccentDim = Color(0xFF388E5E)
    val White     = Color(0xFFFFFFFF)
    val Grey50    = Color(0xFFF4FAF6)   // very faint green tint on surface
    val Grey100   = Color(0xFFE8F5EC)
    val Grey200   = Color(0xFFD0E8D8)
    val Grey300   = Color(0xFFAAD0B8)
    val Grey400   = Color(0xFF84B296)
    val Grey500   = Color(0xFF5E8E72)
    val Grey600   = Color(0xFF456A56)
    val Grey700   = Color(0xFF2E4A3C)
    val Grey800   = Color(0xFF1C2E26)
    val Success   = Color(0xFF2ECC8C)
    val Warning   = Color(0xFFFFB547)
    val Error     = Color(0xFFFF5F6D)
    val Profit    = Color(0xFF2ECC8C)
    val Loss      = Color(0xFFFF5F6D)
    val Surface   = Color(0xFFF4FAF6)   // faint green tint background
    val Card      = Color(0xFFFFFFFF)
    val Divider   = Color(0xFFE8F5EC)
    val Gold      = Color(0xFFD4A843)
    val GoldDim   = Color(0xFFB8892A)
}

// ─────────────────────────────────────────────
// MOTION SYSTEM
// ─────────────────────────────────────────────

object PSRMotion {
    val StandardEnter = tween<Float>(300, easing = EaseOutCubic)
    val StandardExit  = tween<Float>(200, easing = EaseInCubic)
    val Emphasized    = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    val Snappy        = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    val SnappyDp      = spring<Dp>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    fun staggerDelay(index: Int, baseDelay: Int = 40): Int = index * baseDelay
    val CounterSpec   = tween<Float>(600, easing = EaseOutExpo)
    val Bounce        = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
}

// ─────────────────────────────────────────────
// SHAPES
// ─────────────────────────────────────────────

object PSRShapes {
    val Card    = RoundedCornerShape(20.dp)
    val CardSm  = RoundedCornerShape(14.dp)
    val Chip    = RoundedCornerShape(10.dp)
    val Button  = RoundedCornerShape(14.dp)
    val Input   = RoundedCornerShape(12.dp)
    val Bottom  = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

// ─────────────────────────────────────────────
// TYPOGRAPHY — Inter (sans) + Playfair (serif)
// ─────────────────────────────────────────────

val PSRTypography = Typography(
    // Display — Montserrat Black for hero numbers (financial totals)
    displayLarge  = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.Black, fontSize = 40.sp, letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.Black, fontSize = 32.sp, letterSpacing = (-1.0).sp),
    displaySmall  = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, letterSpacing = (-0.5).sp),

    // Headlines — Montserrat Bold for section titles / screen headers
    headlineLarge  = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-0.3).sp),
    headlineSmall  = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),

    // Titles — Montserrat SemiBold for cards, nav, stat values
    titleLarge  = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    titleSmall  = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),

    // Body — Inter Regular/Medium for readable list content
    bodyLarge  = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall  = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.1.sp),

    // Labels — Inter for chips, badges, captions
    labelLarge  = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.4.sp),
    labelSmall  = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.6.sp),
)

// ─────────────────────────────────────────────
// ACCENT TEXT STYLES — Bold Italic with colour
// Use these inline: Text("heading", style = PSRTextStyles.AccentBoldItalic)
// ─────────────────────────────────────────────

object PSRTextStyles {
    /** Playfair bold-italic — for invoice header accents, section openers */
    val SerifAccent = TextStyle(fontFamily = PlayfairFamily, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, fontSize = 18.sp, letterSpacing = (-0.3).sp)
    /** Montserrat bold-italic — for stat labels, highlighted numbers */
    val BoldItalic = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, fontSize = 14.sp)
    /** Inter semibold — for captions that need to pop */
    val Caption = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.8.sp)
}

// ─────────────────────────────────────────────
// COLOR SCHEME
// ─────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary             = PSRColors.Navy600,
    onPrimary           = PSRColors.White,
    primaryContainer    = PSRColors.Navy700,
    onPrimaryContainer  = PSRColors.Grey100,
    secondary           = PSRColors.Accent,
    onSecondary         = PSRColors.White,
    secondaryContainer  = Color(0xFFD0E8D8),
    onSecondaryContainer = PSRColors.Navy600,
    background          = PSRColors.Surface,
    onBackground        = PSRColors.Navy900,
    surface             = PSRColors.Card,
    onSurface           = PSRColors.Navy900,
    surfaceVariant      = PSRColors.Grey100,
    onSurfaceVariant    = PSRColors.Grey600,
    outline             = PSRColors.Grey200,
    outlineVariant      = PSRColors.Divider,
    error               = PSRColors.Error,
    onError             = PSRColors.White,
)

// ─────────────────────────────────────────────
// THEME
// ─────────────────────────────────────────────

@Composable
fun PSRTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = PSRTypography,
        shapes      = MaterialTheme.shapes.copy(
            small      = RoundedCornerShape(8.dp),
            medium     = RoundedCornerShape(14.dp),
            large      = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
