package com.tripfair.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tripfair.app.R

// Inter is bundled as one variable font file; each weight picks a point on its "wght" axis.
private fun interFont(weight: FontWeight) = Font(
    resId = R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Inter = FontFamily(
    interFont(FontWeight.Normal),
    interFont(FontWeight.Medium),
    interFont(FontWeight.SemiBold),
    interFont(FontWeight.Bold),
    interFont(FontWeight.ExtraBold),
)

private fun inter(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

private val Default = Typography()

// Sizes and weights follow the Figma screens; the comments name where each style is used.
val TripFairTypography = Typography(
    // Not used in the design yet; kept in Inter so nothing falls back to Roboto.
    displayLarge = Default.displayLarge.copy(fontFamily = Inter),
    displayMedium = Default.displayMedium.copy(fontFamily = Inter),
    // Big result numbers ("90% hài lòng")
    displaySmall = inter(size = 34, lineHeight = 41, weight = FontWeight.Normal),
    // Screen greeting ("Chào mừng trở lại")
    headlineLarge = inter(size = 30, lineHeight = 34, weight = FontWeight.ExtraBold, letterSpacing = -0.8),
    // Place and trip names on detail and hero cards
    headlineMedium = inter(size = 26, lineHeight = 32, weight = FontWeight.ExtraBold),
    // Brand name next to the logo
    headlineSmall = inter(size = 23, lineHeight = 28, weight = FontWeight.ExtraBold, letterSpacing = -0.5),
    // Top bar title
    titleLarge = inter(size = 22, lineHeight = 28, weight = FontWeight.Normal),
    // Section heading
    titleMedium = inter(size = 18, lineHeight = 24, weight = FontWeight.ExtraBold),
    // Card title
    titleSmall = inter(size = 14, lineHeight = 18, weight = FontWeight.Bold),
    // Paragraphs and input text
    bodyLarge = inter(size = 14, lineHeight = 21, weight = FontWeight.Normal),
    bodyMedium = inter(size = 13, lineHeight = 18, weight = FontWeight.Normal),
    // Secondary lines under titles
    bodySmall = inter(size = 12, lineHeight = 17, weight = FontWeight.Normal),
    // Button text
    labelLarge = inter(size = 15, lineHeight = 20, weight = FontWeight.Bold),
    // Input field label
    labelMedium = inter(size = 13, lineHeight = 16, weight = FontWeight.Bold),
    // Captions and hints
    labelSmall = inter(size = 11, lineHeight = 15, weight = FontWeight.Normal),
)
