package com.repsrox.app.data

import java.time.LocalDate
import java.time.ZoneId

/** A best, as the profile board prints it: what it was, what it read, and when. */
data class PersonalRecord(val name: String, val value: String, val whenLabel: String? = null)

/** One loaded set, once the session around it stops mattering. */
private data class Lift(val name: String, val kg: Float, val reps: Int, val day: LocalDate)

/**
 * The heaviest set banked against each lift, read straight off the sessions log.
 *
 * Sets logged in metres carry a sled or a carry load rather than a lift, so they
 * are left out for the same reason [volumeKg] leaves them out of tonnage — the
 * 150 kg under a sled push is not a 150 kg lift. Sets logged without a weight
 * are bodyweight work and hold no record either.
 *
 * A lift is ranked on load first and reps second, so a triple beats a single at
 * the same weight. Where the same set was worked more than once the earliest
 * dates the record, since a best belongs to the day it was first hit rather than
 * the last time it stood.
 *
 * Exercise names are typed rather than picked, so lifts are gathered without
 * regard to case: "Back squat" and "back squat" are one lift, spelled the way
 * the session that set the record spelled it.
 */
fun bestLifts(
    sessions: List<Session>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): List<PersonalRecord> = sessions
    .flatMap { session ->
        val day = session.day(zone)
        session.exercises.flatMap { exercise ->
            exercise.sets.mapNotNull { set ->
                if (set.unit != SetUnit.REPS) return@mapNotNull null
                val kg = set.kg.toFloatOrNull()?.takeIf { it > 0f } ?: return@mapNotNull null
                Lift(exercise.name, kg, set.reps, day)
            }
        }
    }
    .groupBy { it.name.lowercase() }
    .map { (_, worked) -> worked.best() }
    // Heaviest first, and alphabetical between lifts that happen to match, so the
    // board holds still rather than reshuffling on every session banked.
    .sortedWith(compareByDescending<Lift> { it.kg }.thenBy { it.name })
    .map { lift ->
        PersonalRecord(
            name = lift.name,
            value = "${formatLoad(lift.kg)} kg × ${lift.reps}",
            whenLabel = lift.day.recordLabel(today),
        )
    }

/** The set a lift is remembered by: the heaviest, then the longest, then the first. */
private fun List<Lift>.best(): Lift = sortedWith(
    compareByDescending<Lift> { it.kg }.thenByDescending { it.reps }.thenBy { it.day },
).first()

/** "Jun" within this year, "Jun 25" once the record is older than that. */
private fun LocalDate.recordLabel(today: LocalDate): String =
    if (year == today.year) month.shortName() else "${month.shortName()} ${year % 100}"
