package com.repsrox.app.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Fixed sample content, transcribed from the "Reps and Rox App" design. This is
 * the shell's stand-in for a real store — every screen reads from here so the
 * UI can be built and reviewed before any persistence exists.
 */

// ── Strength session ────────────────────────────────────────────────────────

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

/** Seconds per kilometre used to derive the running distance from the clock. */
const val RUN_SECONDS_PER_KM = 312f

// ── Fuel ────────────────────────────────────────────────────────────────────

/**
 * The design's four meals, as the shape a fresh install is seeded with. They
 * carry no date or id of their own — [MealRepository] hangs them off the day
 * the app is first opened.
 */
val MEAL_TEMPLATE = listOf(
    Meal("", LocalDate.MIN, "Breakfast", "Oats, whey, banana", kcal = 620, proteinG = 42, carbsG = 78, logged = true),
    Meal("", LocalDate.MIN, "Lunch", "Rice, chicken, greens", kcal = 780, proteinG = 62, carbsG = 96, logged = true),
    Meal("", LocalDate.MIN, "Dinner", "Salmon, potatoes, salad", kcal = 720, proteinG = 48, carbsG = 74),
    Meal("", LocalDate.MIN, "Post-session", "40 g protein target", kcal = 220, proteinG = 40, carbsG = 22),
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

/**
 * The design's station bests. Unlike the lifts — which [bestLifts] now reads off
 * the sessions log — nothing measures these yet: the live tracker banks a sled
 * push as metres moved and never times the leg, so there is no figure to derive.
 * They stay sample content until it does.
 */
val PR_STATIONS = listOf(
    PersonalRecord("SkiErg 1000 m", "4:02"),
    PersonalRecord("Sled push 50 m", "2:38"),
    PersonalRecord("Burpee broad jump 80 m", "4:04"),
    PersonalRecord("Wall balls 100", "5:46"),
)
