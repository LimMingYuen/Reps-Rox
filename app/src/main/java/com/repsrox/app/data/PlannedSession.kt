package com.repsrox.app.data

import java.time.LocalDate
import java.util.Locale

/** What a session asks of you. */
enum class SessionKind { STRENGTH, RUN, RACE, REST }

/** Where a session stands, read off its date and whether it was finished. */
enum class DayStatus { DONE, TODAY, PLANNED, REST }

/** One prescription inside a session: eight reps at a hundred kilos. */
data class WorkSet(val reps: Int, val kg: String)

/**
 * A movement and the sets planned for it. [target] is the line the session
 * screens print beside the name — "4 × 8 · 100 kg" — held rather than derived so
 * the design's own hand-written targets survive alongside built ones.
 */
data class Exercise(val name: String, val target: String, val sets: List<WorkSet>)

/**
 * A session on the week's plan. Sessions are the one thing besides weigh-ins the
 * app writes to disk, so this carries everything a plan row, the live tracker and
 * the summary need — there is no second lookup once one is loaded.
 */
data class PlannedSession(
    val id: String,
    val date: LocalDate,
    val name: String,
    val kind: SessionKind,
    /** The free line a run or a race is described by, where exercises say nothing. */
    val note: String = "",
    val exercises: List<Exercise> = emptyList(),
    val done: Boolean = false,
) {
    val plannedSets: Int get() = exercises.sumOf { it.sets.size }

    /** Planned tonnage — reps × load over every set that carries one. */
    val volumeKg: Float
        get() = exercises.fold(0f) { total, exercise ->
            total + exercise.sets.fold(0f) { sum, set ->
                sum + set.reps * (set.kg.toFloatOrNull() ?: 0f)
            }
        }

    /**
     * A rest day is always rest; anything else is done once it has been finished,
     * and until then reads as today's or as still ahead. A planned session whose
     * date has passed stays planned rather than claiming it was done.
     */
    fun status(today: LocalDate = LocalDate.now()): DayStatus = when {
        kind == SessionKind.REST -> DayStatus.REST
        done -> DayStatus.DONE
        date == today -> DayStatus.TODAY
        else -> DayStatus.PLANNED
    }

    /** The second line of a plan row: where the session stands, then what it holds. */
    fun meta(today: LocalDate = LocalDate.now()): String {
        val standing = when (status(today)) {
            DayStatus.DONE -> "Done"
            DayStatus.TODAY -> "Today"
            DayStatus.PLANNED -> "Planned"
            DayStatus.REST -> null
        }
        val detail = when {
            note.isNotBlank() -> note
            exercises.isNotEmpty() ->
                "${exercises.size} ${plural(exercises.size, "exercise")} · " +
                    "$plannedSets ${plural(plannedSets, "set")}"
            else -> null
        }
        return listOfNotNull(standing, detail).joinToString(" · ")
    }
}

/** The ranges a built session is held to; outside them the entry is a slip. */
val SETS_RANGE = 1..12
val REPS_RANGE = 1..500
val LOAD_RANGE_KG = 0f..500f
const val NAME_MAX_CHARS = 40

/**
 * Builds an exercise from what the add-exercise dialog collects, writing the same
 * target line the design's own exercises carry. A load of zero reads as bodyweight
 * and is left off both the target and the set chips.
 */
fun buildExercise(name: String, sets: Int, reps: Int, kg: Float): Exercise {
    val load = if (kg > 0f) formatLoad(kg) else ""
    return Exercise(
        name = name,
        target = if (load.isEmpty()) "$sets × $reps" else "$sets × $reps · $load kg",
        sets = List(sets) { WorkSet(reps, load) },
    )
}

/**
 * What an exercise reads as once it has been worked through: "5 / 5 / 5 · 120 kg".
 * A load shared by every set is called once at the end, as it is on paper.
 */
fun Exercise.logLine(): String {
    val reps = sets.joinToString(" / ") { it.reps.toString() }
    val load = sets.map { it.kg }.distinct().singleOrNull()?.takeIf { it.isNotBlank() }
    return if (load == null) reps else "$reps · $load kg"
}

/** Whole kilos stay whole — "24", not "24.0" — since that is how loads are called. */
fun formatLoad(kg: Float): String =
    if (kg % 1f == 0f) kg.toInt().toString() else String.format(Locale.US, "%.1f", kg)

/** Planned volume, in the tonnes the design's stat blocks show. */
fun formatTonnes(kg: Float): String = String.format(Locale.US, "%.1f t", kg / 1000f)

internal fun plural(count: Int, noun: String): String = if (count == 1) noun else "${noun}s"
