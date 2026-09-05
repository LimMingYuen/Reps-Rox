package com.repsrox.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette lifted from the "Reps and Rox App" design. The design overrides the
 * Nocturne design system's own blurple-on-indigo ground with its own near-black
 * ground and a single hot-orange accent, so these are the values that govern.
 */

/** #ff3b14 — the one accent. Used as a line, a mark and a tint, never a flood. */
val Accent = Color(0xFFFF3B14)
val AccentTint = Color(0x24FF3B14) // rgba(255,59,20,.14) — filled action
val AccentWash = Color(0x1AFF3B14) // rgba(255,59,20,.10) — banner ground
val AccentSet = Color(0x29FF3B14) // rgba(255,59,20,.16) — banked set chip
val AccentLine = Color(0x80FF3B14) // rgba(255,59,20,.50)
val AccentLineSoft = Color(0x59FF3B14) // rgba(255,59,20,.35)
val AccentLineFaint = Color(0x4DFF3B14) // rgba(255,59,20,.30)
val AccentRingNow = Color(0x66FF3B14) // rgba(255,59,20,.40)
val AccentArea = Color(0x24FF3B14) // weight chart fill

/** Grounds. */
val ScreenBg = Color(0xFF101113) // the app canvas
val SurfaceBg = Color(0xFF1A1B1E) // cards
val SurfaceRaised = Color(0xFF22242A) // set chips, avatar
val NavBg = Color(0xFF131417) // bottom bar

/** Text ramp — one ink at descending opacity, as the design does it. */
val TextPrimary = Color(0xFFE9E9ED)
val TextMuted = Color(0x8CE9E9ED) // .55
val TextSubtle = Color(0x99E9E9ED) // .60
val TextSecondary = Color(0x80E9E9ED) // .50
val TextMeta = Color(0x73E9E9ED) // .45
val TextFaint = Color(0x66E9E9ED) // .40
val TextDim = Color(0x59E9E9ED) // .35
val TextOnAction = Color(0xB3E9E9ED) // .70

/** Edges and rules. */
val Hairline = Color(0x14E9E9ED) // .08 — list row rules
val BorderSoft = Color(0x1AE9E9ED) // .10 — card outlines
val BorderChip = Color(0x1FE9E9ED) // .12
val BorderAction = Color(0x33E9E9ED) // .20 — secondary buttons
val TrackFaint = Color(0x14E9E9ED) // .08 — split bars
val TrackSoft = Color(0x17E9E9ED) // .09 — macro bars
val Track = Color(0x1AE9E9ED) // .10 — protein bar
val RingTodo = Color(0x1FE9E9ED) // .12
