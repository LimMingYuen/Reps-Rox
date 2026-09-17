package com.repsrox.app.data

import java.time.Instant

/**
 * One sim as it was actually raced: the clock it ended on and every leg closed,
 * in course order. A sim ended early holds fewer legs than [LEGS], and its clock
 * can run past their sum — the leg still open when it ended is time raced too.
 */
data class RaceResult(
    val finishedAt: Instant,
    val seconds: Int,
    val legs: List<Int>,
)

/** True when every leg was closed, which is the only sim whose time is a finish time. */
val RaceResult.complete: Boolean get() = legs.size == LEGS.size

/** Stations closed — the figure a sim ended early is read by. */
val RaceResult.stationsClosed: Int
    get() = legs.indices.count { LEGS[it].isStation }

/** The quickest complete sim in the log, which is the one worth marking. */
fun bestRace(log: List<RaceResult>): RaceResult? =
    log.filter { it.complete }.minByOrNull { it.seconds }

// ── Codec ───────────────────────────────────────────────────────────────────

/** A record is `epochSecond|seconds|leg,leg,…`: `1755302400|5412|312,265,…`. */
internal fun encodeRaceResult(result: RaceResult): String =
    "${result.finishedAt.epochSecond}|${result.seconds}|${result.legs.joinToString(",")}"

internal fun encodeRaceLog(log: List<RaceResult>): String =
    log.joinToString("\n", transform = ::encodeRaceResult)

/** Skips anything it cannot read rather than losing the whole log to one bad record. */
internal fun decodeRaceLog(raw: String): List<RaceResult> = raw.lineSequence()
    .mapNotNull(::decodeRaceResult)
    .sortedByDescending { it.finishedAt }
    .toList()

internal fun decodeRaceResult(line: String): RaceResult? {
    val parts = line.split('|')
    if (parts.size < 3) return null
    val epoch = parts[0].toLongOrNull() ?: return null
    val seconds = parts[1].toIntOrNull() ?: return null
    val legs = parts[2].split(',').filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: return null }
    // More legs than the course has is a record from some other course.
    if (seconds <= 0 || legs.size > LEGS.size) return null
    return RaceResult(Instant.ofEpochSecond(epoch), seconds, legs)
}
