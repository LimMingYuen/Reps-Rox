package com.repsrox.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * The design is dark-only and brand-coloured, so there is no light scheme and
 * no dynamic colour — the accent is the identity.
 */
private val RepsRoxColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = ScreenBg,
    secondary = Accent,
    onSecondary = ScreenBg,
    background = ScreenBg,
    onBackground = TextPrimary,
    surface = SurfaceBg,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextMuted,
    outline = BorderSoft,
    outlineVariant = Hairline,
)

@Composable
fun RepsRoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RepsRoxColorScheme,
        typography = Typography,
        content = content,
    )
}
