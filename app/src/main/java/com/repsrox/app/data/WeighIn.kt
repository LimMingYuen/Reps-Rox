package com.repsrox.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** One morning on the scales. */
data class WeighIn(
    val date: LocalDate,
    val kg: Float,
)

/** The plausible range for a logged weight; anything outside is a typo. */
val WEIGHT_RANGE_KG = 25f..300f

/** The window the Body screen's trend covers, when there is enough history for it. */
const val TREND_WEEKS = 12L

/** Everything the Body screen reads off the log, derived in one pass. */
data class WeightSummary(
    val latest: WeighIn,
    /** How long ago the latest weigh-in was, in the design's own phrasing. */
    val latestLabel: String,
    /** Whole weeks the trend spans — up to [TREND_WEEKS], less while history is short. */
    val trendWeeks: Long,
    /** Weight change across that span, negative for weight lost. Null until two weigh-ins exist. */
    val trendChange: Float?,
    /** Mean of the weigh-ins in the last seven days, null if the latest is older than that. */
    val sevenDayAverage: Float?,
    /** Change against the weigh-in from a fortnight back — the figure Today's tile shows. */
    val fortnightChange: Float?,
)

/**
 * Reduces the log to the numbers the Body screen shows. [entries] must be
 * oldest-first, which is the order [WeightRepository] keeps them in.
 */
fun summarise(entries: List<WeighIn>, today: LocalDate = LocalDate.now()): WeightSummary? {
    val latest = entries.lastOrNull() ?: return null

    // The trend runs back twelve weeks from the latest weigh-in, or to the start
    // of the log while it is shorter than that.
    val windowStart = latest.date.minusWeeks(TREND_WEEKS)
    val oldestInWindow = entries.firstOrNull { !it.date.isBefore(windowStart) } ?: latest

    val week = entries.filter { it.date > latest.date.minusDays(7) }

    // The most recent weigh-in that is already a fortnight old; without one there
    // is no fortnight to report on.
    val fortnightAgo = entries.lastOrNull { it.date <= latest.date.minusDays(14) }

    return WeightSummary(
        latest = latest,
        latestLabel = when (latest.date) {
            today -> "this morning"
            today.minusDays(1) -> "yesterday"
            else -> "${latest.date.dayOfMonth} ${latest.date.month.shortName()}"
        },
        trendWeeks = ChronoUnit.WEEKS.between(oldestInWindow.date, latest.date),
        trendChange = if (oldestInWindow === latest) null else latest.kg - oldestInWindow.kg,
        // A lone weigh-in is its own average, which says nothing; wait for a second.
        sevenDayAverage = if (week.size < 2) null else week.map { it.kg }.average().toFloat(),
        fortnightChange = fortnightAgo?.let { latest.kg - it.kg },
    )
}

/** "May", "Jun" — the abbreviations the chart's axis uses. */
fun java.time.Month.shortName(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

/** One decimal, and the design's true minus sign rather than a hyphen. */
fun formatKilos(kg: Float): String = String.format(java.util.Locale.US, "%.1f", kg)

/** A signed change: "−2.8", "+0.4", "0.0". */
fun formatSigned(kg: Float): String {
    val sign = when {
        kg <= -0.05f -> "−"
        kg >= 0.05f -> "+"
        else -> ""
    }
    return "$sign${formatKilos(abs(kg))}"
}

/** The same, in kilograms: "−2.8 kg". */
fun formatChange(kg: Float): String = "${formatSigned(kg)} kg"
