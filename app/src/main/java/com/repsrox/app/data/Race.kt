package com.repsrox.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * The race being trained for. One at a time: the profile asks what is next, and
 * a second date behind the first says nothing a training block can act on.
 */
data class Race(val name: String, val date: LocalDate)

const val RACE_NAME_MAX_CHARS = 40

/** The name a record carrying none falls back to, so a bad line still reads. */
private const val UNNAMED_RACE = "Race"

/**
 * How far off a day is, in the phrasing the profile line uses. Weeks take over
 * from days at three, where counting days stops being how anyone thinks about
 * it, and the same phrasing runs backwards for a race already run.
 */
fun countdownTo(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return when (days) {
        0L -> "today"
        1L -> "tomorrow"
        -1L -> "yesterday"
        else -> {
            val span = abs(days)
            val figure = if (span < 21L) "$span days" else "${span / 7} weeks"
            if (days > 0L) "in $figure" else "$figure ago"
        }
    }
}

/** "14 Nov", carrying the year once the race is not in this one. */
fun raceDateLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val day = "${date.dayOfMonth} ${date.month.shortName()}"
    return if (date.year == today.year) day else "$day ${date.year}"
}

fun Race.countdown(today: LocalDate = LocalDate.now()): String = countdownTo(date, today)

fun Race.dateLabel(today: LocalDate = LocalDate.now()): String = raceDateLabel(date, today)

// ── Codec ───────────────────────────────────────────────────────────────────

/** The separator is structure, so a name carrying one is stripped rather than allowed to break the record. */
internal fun encodeRace(race: Race): String =
    "${race.date}|${race.name.filterNot { it == '|' || it == '\n' }.trim()}"

/** Returns null rather than a half-read race, which the screen shows as nothing booked. */
internal fun decodeRace(raw: String): Race? {
    val date = runCatching { LocalDate.parse(raw.substringBefore('|')) }.getOrNull() ?: return null
    val name = raw.substringAfter('|', missingDelimiterValue = "").trim()
    return Race(name.ifBlank { UNNAMED_RACE }, date)
}
