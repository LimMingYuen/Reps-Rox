package com.repsrox.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.repsrox.app.R

/**
 * Both faces ship as variable fonts. Passing a [FontWeight] to [Font] is enough:
 * from Compose 1.7 the default `variationSettings` are derived from it, so each
 * entry pins the `wght` axis to its own instance on API 26+.
 */
val Oswald = FontFamily(
    Font(R.font.oswald_variable, FontWeight.W400),
    Font(R.font.oswald_variable, FontWeight.W500),
    Font(R.font.oswald_variable, FontWeight.W600),
    Font(R.font.oswald_variable, FontWeight.W700),
)

val Inter = FontFamily(
    Font(R.font.inter_variable, FontWeight.W400),
    Font(R.font.inter_variable, FontWeight.W500),
    Font(R.font.inter_variable, FontWeight.W600),
)

/**
 * The design sizes type per-element rather than from a scale, so these are
 * builders rather than a fixed set of roles. [lineHeight] and [tracking] are
 * multiples of the font size, matching the CSS `font:` shorthand and `em`
 * letter-spacing the design is written in.
 */
fun oswald(
    size: Float,
    weight: FontWeight = FontWeight.W600,
    lineHeight: Float = 1f,
    tracking: Float = 0f,
) = TextStyle(
    fontFamily = Oswald,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = (size * tracking).sp,
)

fun inter(
    size: Float,
    weight: FontWeight = FontWeight.W400,
    lineHeight: Float = 1.4f,
    tracking: Float = 0f,
) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = (size * tracking).sp,
)

/** Numeric metadata — the design sets these in the UI monospace stack. */
fun mono(
    size: Float,
    weight: FontWeight = FontWeight.W400,
) = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = size.sp,
)

/** The recurring all-caps section label: Inter 10/400 at .14em. */
val Kicker = inter(10f, FontWeight.W400, lineHeight = 1f, tracking = 0.14f)

val Typography = Typography(
    bodyLarge = inter(15f, FontWeight.W400, lineHeight = 1.55f),
    bodyMedium = inter(13f, FontWeight.W400, lineHeight = 1.5f),
    labelSmall = Kicker,
)
