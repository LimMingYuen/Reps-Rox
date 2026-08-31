package com.repsrox.app.data

import java.time.DayOfWeek

/**
 * Fixed sample content, transcribed from the "Reps and Rox App" design. This is
 * the shell's stand-in for a real store — every screen reads from here so the
 * UI can be built and reviewed before any persistence exists.
 */

// ── Strength session ────────────────────────────────────────────────────────

/**
 * Where the live session clock starts, in seconds. The design opens the app
 * part-way through a session rather than at zero, and a session started after
 * one is banked begins at nothing.
 */
const val LIVE_ELAPSED = 1877

// ── Race simulation ─────────────────────────────────────────────────────────

data class Leg(val tag: String, val name: String, val target: String) {
    val isStation: Boolean get() = tag.startsWith("STN")
}

val LEGS = listOf(
    Leg("RUN 1", "1 km run", "1 km · target 4:35"),
    Leg("STN 1", "SkiErg", "1000 m"),
    Leg("RUN 2", "1 km run", "1 km · target 4:40"),
    Leg("STN 2", "Sled push", "50 m · 152 kg"),
    Leg("RUN 3", "1 km run", "1 km · target 4:45"),
    Leg("STN 3", "Sled pull", "50 m · 103 kg"),
    Leg("RUN 4", "1 km run", "1 km · target 4:45"),
    Leg("STN 4", "Burpee broad jump", "80 m"),
    Leg("RUN 5", "1 km run", "1 km · target 4:50"),
    Leg("STN 5", "Rowing", "1000 m"),
    Leg("RUN 6", "1 km run", "1 km · target 4:50"),
    Leg("STN 6", "Farmers carry", "200 m · 2 × 24 kg"),
    Leg("RUN 7", "1 km run", "1 km · target 4:55"),
    Leg("STN 7", "Sandbag lunges", "100 m · 20 kg"),
    Leg("RUN 8", "1 km run", "1 km · target 5:00"),
    Leg("STN 8", "Wall balls", "100 reps · 9 kg"),
)

val LEG_TIMES = listOf(252, 275, 278, 172, 285, 208, 288, 244, 292, 268, 295, 176, 298, 232, 302, 366)

val LEG_DELTA = listOf(
    "−0:03", "+0:12", "+0:03", "−0:08", "+0:10", "+0:04", "0:00", "+0:19",
    "+0:07", "−0:05", "+0:12", "−0:11", "+0:14", "+0:06", "+0:18", "+0:24",
)

/** The sled the race pushes, in kilograms — mirrors the STN 2 leg above. */
const val RACE_SLED_KG = 152f

const val ROXZONE_TOTAL = "3:42"
const val RACE_RUN_AVG = "4:38"
const val RACE_PROJECTED = "1:22:40"

// ── Week plan ───────────────────────────────────────────────────────────────

/**
 * A first-run week, one session a day, held against a weekday rather than a date
 * so [PlanRepository] can lay it onto whichever week the app is first opened in.
 */
val WEEK_TEMPLATE = listOf(
    TemplateSession(
        DayOfWeek.MONDAY, "Upper pull + carries", SessionKind.STRENGTH,
        exercises = listOf(
            buildExercise("Barbell row", sets = 4, reps = 8, kg = 70f),
            buildExercise("Lat pulldown", sets = 4, reps = 10, kg = 60f),
            buildExercise("Farmers carry", sets = 4, reps = 40, kg = 32f, unit = SetUnit.METRES),
            buildExercise("Dumbbell row", sets = 3, reps = 10, kg = 30f),
            buildExercise("Face pull", sets = 3, reps = 15, kg = 20f),
        ),
    ),
    TemplateSession(DayOfWeek.TUESDAY, "Rest", SessionKind.REST, note = "Walk 6 km"),
    TemplateSession(DayOfWeek.WEDNESDAY, "Compromised running", SessionKind.RUN, note = "4 × (1 km + 20 wall balls)"),
    TemplateSession(DayOfWeek.THURSDAY, "8 km Zone 2", SessionKind.RUN, note = "42:00"),
    TemplateSession(DayOfWeek.FRIDAY, "Rest", SessionKind.REST, note = "Walk 6 km"),
    TemplateSession(
        DayOfWeek.SATURDAY, "Lower push + sled finisher", SessionKind.STRENGTH,
        exercises = listOf(
            buildExercise("Back squat", sets = 5, reps = 5, kg = 120f),
            buildExercise("Romanian deadlift", sets = 4, reps = 8, kg = 100f),
            buildExercise("Bulgarian split squat", sets = 3, reps = 10, kg = 24f),
            buildExercise("Sled push", sets = 4, reps = 25, kg = 150f, unit = SetUnit.METRES),
            buildExercise("Wall balls", sets = 2, reps = 50, kg = 9f),
        ),
    ),
    TemplateSession(DayOfWeek.SUNDAY, "Long Z2", SessionKind.RUN, note = "75 min"),
)

// ── Today ───────────────────────────────────────────────────────────────────

// ── Run ─────────────────────────────────────────────────────────────────────

/** Bar heights are in the design's own px, drawn against a 52px column. */
data class HeartRateZone(val label: String, val height: Float)

val ZONES = listOf(
    HeartRateZone("Z1", 9f),
    HeartRateZone("Z2", 44f),
    HeartRateZone("Z3", 20f),
    HeartRateZone("Z4", 7f),
    HeartRateZone("Z5", 3f),
)

const val ZONE_COLUMN_HEIGHT = 52f

data class Split(val km: String, val pace: String, val width: Int)

val RUN_SPLITS = listOf(
    Split("1", "5:22", 68), Split("2", "5:16", 74),
    Split("3", "5:09", 82), Split("4", "5:11", 80),
    Split("5", "5:04", 88), Split("6", "5:08", 84),
    Split("7", "4:58", 96), Split("8", "5:12", 78),
)

/** Seconds per kilometre used to derive the running distance from the clock. */
const val RUN_SECONDS_PER_KM = 312f

// ── Fuel ────────────────────────────────────────────────────────────────────

data class Macro(val label: String, val value: String, val percent: Int, val accented: Boolean)

val MACROS = listOf(
    Macro("Calories", "2,180 / 2,540", 86, accented = false),
    Macro("Protein", "148 / 165 g", 90, accented = true),
    Macro("Carbs", "244 / 290 g", 84, accented = false),
)

data class Meal(val key: String, val name: String, val meta: String, val kcal: String)

val MEALS = listOf(
    Meal("b", "Breakfast", "Oats, whey, banana", "620"),
    Meal("l", "Lunch", "Rice, chicken, greens", "780"),
    Meal("d", "Dinner", "Not logged", "—"),
    Meal("s", "Post-session", "40 g protein target", "—"),
)

// ── Body ────────────────────────────────────────────────────────────────────

/**
 * The design's twelve weigh-ins. Now that the Body screen keeps a real log,
 * these are only what [WeightRepository] seeds a fresh install with.
 */
val WEIGHT_SERIES = listOf(
    84.2f, 84.0f, 83.5f, 83.6f, 83.1f, 82.8f, 82.9f, 82.4f, 82.1f, 81.9f, 81.6f, 81.4f,
)

// ── Profile ─────────────────────────────────────────────────────────────────

data class PersonalRecord(val name: String, val value: String, val whenLabel: String? = null)

val PR_LIFTS = listOf(
    PersonalRecord("Back squat", "150 kg", "Jun"),
    PersonalRecord("Deadlift", "185 kg", "Jul"),
    PersonalRecord("Bench press", "105 kg", "May"),
    PersonalRecord("5 km run", "22:41", "Aug"),
)

val PR_STATIONS = listOf(
    PersonalRecord("SkiErg 1000 m", "4:02"),
    PersonalRecord("Sled push 50 m", "2:38"),
    PersonalRecord("Burpee broad jump 80 m", "4:04"),
    PersonalRecord("Wall balls 100", "5:46"),
)
