package com.repsrox.app.data

/**
 * Fixed sample content, transcribed from the "Reps and Rox App" design. This is
 * the shell's stand-in for a real store — every screen reads from here so the
 * UI can be built and reviewed before any persistence exists.
 */

// ── Strength session ────────────────────────────────────────────────────────

data class WorkSet(val reps: Int, val kg: String)

data class Exercise(val name: String, val target: String, val sets: List<WorkSet>)

val EXERCISES = listOf(
    Exercise(
        "Back squat", "5 × 5 · 120 kg",
        List(5) { WorkSet(5, "120") },
    ),
    Exercise(
        "Romanian deadlift", "4 × 8 · 100 kg",
        List(4) { WorkSet(8, "100") },
    ),
    Exercise(
        "Bulgarian split squat", "3 × 10 · 24 kg",
        List(3) { WorkSet(10, "24") },
    ),
    Exercise(
        "Sled push", "4 × 25 m · 150 kg",
        List(4) { WorkSet(25, "150") },
    ),
    Exercise(
        "Wall balls", "2 × 50 · 9 kg",
        List(2) { WorkSet(50, "9") },
    ),
)

/** The design's fixed elapsed clock for the live session, in seconds. */
const val LIVE_ELAPSED = 1877

const val PLANNED_SETS = 18

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

enum class SessionKind { STRENGTH, RUN, RACE, REST }

enum class DayStatus { DONE, TODAY, PLANNED, REST }

data class PlannedDay(
    val day: String,
    val date: String,
    val name: String,
    val meta: String,
    val status: DayStatus,
    val kind: SessionKind,
)

val WEEK = listOf(
    PlannedDay("MON", "11", "Upper pull + carries", "Done · 51:04 · 3.8 t", DayStatus.DONE, SessionKind.STRENGTH),
    PlannedDay("TUE", "12", "Rest", "Walk 6 km", DayStatus.REST, SessionKind.REST),
    PlannedDay("WED", "13", "Compromised running", "Done · 4 × (1 km + 20 wall balls)", DayStatus.DONE, SessionKind.RUN),
    PlannedDay("THU", "14", "8 km Zone 2", "Done · 42:18", DayStatus.DONE, SessionKind.RUN),
    PlannedDay("SAT", "15", "Lower push + sled finisher", "Today · 5 exercises · 18 sets", DayStatus.TODAY, SessionKind.STRENGTH),
    PlannedDay("SUN", "16", "Long Z2 · 14 km", "Planned · 75 min", DayStatus.PLANNED, SessionKind.RUN),
    PlannedDay("MON", "17", "Full race sim", "Planned · 8 stations", DayStatus.PLANNED, SessionKind.RACE),
)

// ── Today ───────────────────────────────────────────────────────────────────

data class RecentSession(
    val kind: SessionKind,
    val name: String,
    val meta: String,
    val whenLabel: String,
)

val RECENT = listOf(
    RecentSession(SessionKind.RUN, "8 km Zone 2", "42:18 · 5:17 /km · 148 bpm", "Thu"),
    RecentSession(SessionKind.STRENGTH, "Upper pull + carries", "51:04 · 3.8 t · 16 sets", "Wed"),
    RecentSession(SessionKind.RACE, "Half-race sim", "38:52 · 4 stations", "Sun"),
)

const val SESSIONS_DONE = 3
const val SESSIONS_PLANNED = 5

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

// ── Summary ─────────────────────────────────────────────────────────────────

data class SummaryRow(val name: String, val detail: String)

val SUMMARY_ROWS = listOf(
    SummaryRow("Back squat", "5 / 5 / 5 / 5 / 5 · 120 kg"),
    SummaryRow("Romanian deadlift", "8 / 8 / 8 / 8 · 100 kg"),
    SummaryRow("Bulgarian split squat", "10 / 10 / 10 · 24 kg"),
    SummaryRow("Sled push", "4 × 25 m · 150 kg"),
    SummaryRow("Wall balls", "50 / 50 · 9 kg"),
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
