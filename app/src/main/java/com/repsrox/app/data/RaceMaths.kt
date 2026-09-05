package com.repsrox.app.data

/**
 * What the race board can say from the clock alone. Every figure here is
 * measured off legs the racer actually closed, so a sim that has not started
 * reports nothing rather than a number it cannot stand behind.
 *
 * `closed` is always the legs closed so far, in course order, which is what
 * makes each leg's index its position in [LEGS].
 */

/** A closed leg against its own target. Negative is under, which is the win. */
fun legDelta(index: Int, seconds: Int): Int = seconds - LEGS[index].targetSeconds

private fun average(of: List<Int>): Int? =
    if (of.isEmpty()) null else of.sum() / of.size

/** Null until a station has been closed — an average of nothing is not zero. */
fun stationAverage(closed: List<Int>): Int? =
    average(closed.filterIndexed { index, _ -> LEGS[index].isStation })

fun runAverage(closed: List<Int>): Int? =
    average(closed.filterIndexed { index, _ -> !LEGS[index].isStation })

/** The whole course as planned, which is what a sim projects to before it starts. */
val PLANNED_FINISH: Int = LEGS.sumOf { it.targetSeconds }

/**
 * Where this sim finishes if the rest of the course is raced at the pace of
 * what is already closed. With nothing closed there is no pace to read, so it
 * projects the plan.
 *
 * Never reports a finish earlier than the clock already shows: a long leg still
 * in progress is time spent, whatever the closed legs suggest.
 */
fun projectedFinish(raceSeconds: Int, closed: List<Int>): Int {
    val closedTarget = closed.indices.sumOf { LEGS[it].targetSeconds }
    if (closedTarget == 0) return maxOf(PLANNED_FINISH, raceSeconds)
    val pace = closed.sum().toDouble() / closedTarget
    val projected = closed.sum() + (PLANNED_FINISH - closedTarget) * pace
    return maxOf(projected.toInt(), raceSeconds)
}
