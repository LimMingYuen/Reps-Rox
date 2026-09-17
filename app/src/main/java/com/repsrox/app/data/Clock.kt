package com.repsrox.app.data

/**
 * How the app prints a clock. These live in `data` rather than beside the
 * screens because the race maths formats its own figures, and a target printed
 * on a leg has to read the same as the split measured against it.
 */

/** m:ss — the short clock, for a split or a leg. */
fun formatMinutes(total: Int): String =
    "${total / 60}:${(total % 60).toString().padStart(2, '0')}"

/** h:mm:ss — the race clock. */
fun formatHours(total: Int): String {
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}

/**
 * A split against its target, signed. Uses a true minus rather than a hyphen,
 * so the column lines up under a proportional face.
 */
fun formatDelta(delta: Int): String = when {
    delta == 0 -> "0:00"
    delta < 0 -> "\u2212${formatMinutes(-delta)}"
    else -> "+${formatMinutes(delta)}"
}
