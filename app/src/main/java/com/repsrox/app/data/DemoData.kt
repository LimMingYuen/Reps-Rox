package com.repsrox.app.data

/**
 * The fixed figures the app measures against, transcribed from the "Reps and
 * Rox App" design: the HYROX course itself and the constants the run and race
 * screens read off it. None of it stands in for a store — what the athlete
 * does is kept by the repositories, and nothing here is seeded into them.
 */

// ── Race simulation ─────────────────────────────────────────────────────────

/**
 * One leg of the course. [targetSeconds] is the time to beat and the only
 * figure the splits measure against, so what a leg prints and what the maths
 * uses cannot drift apart.
 */
data class Leg(val tag: String, val name: String, val brief: String, val targetSeconds: Int) {
    val isStation: Boolean get() = tag.startsWith("STN")

    /** What the board prints under the name: the brief, then the time to beat. */
    val target: String get() = "$brief · target ${formatMinutes(targetSeconds)}"
}

val LEGS = listOf(
    Leg("RUN 1", "1 km run", "1 km", 275),
    Leg("STN 1", "SkiErg", "1000 m", 270),
    Leg("RUN 2", "1 km run", "1 km", 280),
    Leg("STN 2", "Sled push", "50 m · 152 kg", 150),
    Leg("RUN 3", "1 km run", "1 km", 285),
    Leg("STN 3", "Sled pull", "50 m · 103 kg", 210),
    Leg("RUN 4", "1 km run", "1 km", 285),
    Leg("STN 4", "Burpee broad jump", "80 m", 270),
    Leg("RUN 5", "1 km run", "1 km", 290),
    Leg("STN 5", "Rowing", "1000 m", 270),
    Leg("RUN 6", "1 km run", "1 km", 290),
    Leg("STN 6", "Farmers carry", "200 m · 2 × 24 kg", 165),
    Leg("RUN 7", "1 km run", "1 km", 295),
    Leg("STN 7", "Sandbag lunges", "100 m · 20 kg", 240),
    Leg("RUN 8", "1 km run", "1 km", 300),
    Leg("STN 8", "Wall balls", "100 reps · 9 kg", 330),
)

/** The sled the race pushes, in kilograms — mirrors the STN 2 leg above. */
const val RACE_SLED_KG = 152f

/** Seconds per kilometre used to derive the running distance from the clock. */
const val RUN_SECONDS_PER_KM = 312f

