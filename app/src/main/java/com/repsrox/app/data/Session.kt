package com.repsrox.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * One exercise as it was actually worked. Only the sets that were banked are
 * here, and they carry the figures they were logged at rather than a reference
 * back into the plan it came from — editing the plan later must not rewrite what you did.
 */
data class LoggedExercise(val name: String, val sets: List<WorkSet>)

/** A strength session, as banked from the live screen. */
data class Session(
    val finishedAt: Instant,
    val name: String,
    val seconds: Int,
    val exercises: List<LoggedExercise>,
)

/** The load one set moved. Metres are a distance, not a rep count, so they move none. */
val WorkSet.volumeKg: Float
    get() = if (unit == SetUnit.REPS) reps * (kg.toFloatOrNull() ?: 0f) else 0f

val Session.totalSets: Int get() = exercises.sumOf { it.sets.size }

/**
 * Tonnage moved. The sled and the carries are logged in metres and contribute
 * nothing — counting them would bank 150 kg twenty-five times per length.
 */
val Session.volumeKg: Float
    get() = exercises.sumOf { exercise -> exercise.sets.sumOf { it.volumeKg.toDouble() } }.toFloat()

/** The heaviest loaded set of the session, which is the one worth calling out. */
data class TopSet(val exercise: String, val set: WorkSet)

val Session.topSet: TopSet?
    get() = exercises
        .flatMap { exercise -> exercise.sets.map { TopSet(exercise.name, it) } }
        .filter { it.set.unit == SetUnit.REPS }
        .maxByOrNull { it.set.kg.toFloatOrNull() ?: 0f }

/** "7.8t" above a tonne, "840 kg" below it — the design's short volume figure. */
fun formatVolume(kg: Float): String = when {
    kg >= 1000f -> String.format(Locale.US, "%.1ft", kg / 1000f)
    else -> "${kg.toInt()} kg"
}

/**
 * How the summary lists an exercise: "5 / 5 / 5 · 120 kg" for even loading,
 * "4 × 25 m · 150 kg" for a carry, and the sets spelled out one by one when the
 * weight moved between them.
 */
fun LoggedExercise.detail(): String {
    if (sets.isEmpty()) return "—"
    val kg = sets.first().kg
    val evenlyLoaded = sets.all { it.kg == kg }

    return when {
        !evenlyLoaded -> sets.joinToString(" / ") { "${it.reps}×${it.kg}" }
        sets.first().unit == SetUnit.METRES -> "${sets.size} × ${sets.first().reps} m · $kg kg"
        else -> sets.joinToString(" / ") { "${it.reps}" } + " · $kg kg"
    }
}

/** The day the session was banked on, read in the phone's own zone. */
fun Session.day(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    finishedAt.atZone(zone).toLocalDate()

/**
 * How a list dates a banked session: "today" and "yesterday" while it is fresh,
 * the weekday within the week, and a date once it is older than that.
 */
fun Session.dayLabel(
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val day = day(zone)
    return when {
        day == today -> "today"
        day == today.minusDays(1) -> "yesterday"
        day.isAfter(today.minusDays(7)) -> day.dayOfWeek.shortName()
        else -> "${day.dayOfMonth} ${day.month.shortName()}"
    }
}

/** "Mon", "Thu" — the abbreviations the recent list uses. */
fun java.time.DayOfWeek.shortName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
