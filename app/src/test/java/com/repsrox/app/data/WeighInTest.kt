package com.repsrox.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WeightLogCodecTest {

    private val date = LocalDate.of(2026, 8, 15)

    @Test
    fun `round trips a log`() {
        val log = listOf(
            WeighIn(date.minusWeeks(1), 82.1f),
            WeighIn(date, 81.4f),
        )
        assertEquals(log, decodeLog(encodeLog(log)))
    }

    @Test
    fun `keeps the readable records and drops the rest`() {
        val raw = """
            2026-08-08|82.1
            not-a-date|82.0
            2026-08-15|81.4
            2026-08-16|heavy
        """.trimIndent()

        assertEquals(
            listOf(WeighIn(date.minusWeeks(1), 82.1f), WeighIn(date, 81.4f)),
            decodeLog(raw),
        )
    }

    @Test
    fun `still reads logs written while weigh-ins carried a waist`() {
        assertEquals(
            listOf(WeighIn(date, 81.4f)),
            decodeLog("2026-08-15|81.4|83.0"),
        )
    }

    @Test
    fun `decodes an emptied log as empty, not as a bad record`() {
        assertEquals(emptyList<WeighIn>(), decodeLog(""))
    }

    @Test
    fun `returns entries oldest first however they were stored`() {
        val raw = "2026-08-15|81.4|\n2026-08-01|83.0|"
        assertEquals(listOf(83.0f, 81.4f), decodeLog(raw).map { it.kg })
    }
}

class WeightSummaryTest {

    private val today = LocalDate.of(2026, 8, 15)

    /** Twelve weekly weigh-ins ending today — the shape a fresh install starts with. */
    private val weekly = WEIGHT_SERIES.mapIndexed { index, kg ->
        WeighIn(today.minusWeeks((WEIGHT_SERIES.lastIndex - index).toLong()), kg)
    }

    @Test
    fun `an empty log has nothing to summarise`() {
        assertNull(summarise(emptyList(), today))
    }

    @Test
    fun `reads the trend off the weigh-ins inside the window`() {
        val summary = summarise(weekly, today)!!

        assertEquals(81.4f, summary.latest.kg, 0.001f)
        assertEquals("this morning", summary.latestLabel)
        // Twelve weekly weigh-ins span eleven weeks between the first and the last.
        assertEquals(11L, summary.trendWeeks)
        assertEquals(-2.8f, summary.trendChange!!, 0.001f)
    }

    @Test
    fun `reads the fortnight off the weigh-in from two weeks back`() {
        // 81.4 today against the 81.9 logged a fortnight ago.
        assertEquals(-0.5f, summarise(weekly, today)!!.fortnightChange!!, 0.001f)
    }

    @Test
    fun `has no fortnight to report until the log is that old`() {
        val recent = listOf(
            WeighIn(today.minusDays(3), 81.6f),
            WeighIn(today, 81.4f),
        )
        assertNull(summarise(recent, today)!!.fortnightChange)
    }

    @Test
    fun `ignores weigh-ins older than the twelve week window`() {
        val withAncient = listOf(WeighIn(today.minusWeeks(60), 95f)) + weekly
        val summary = summarise(withAncient, today)!!

        assertEquals(11L, summary.trendWeeks)
        assertEquals(-2.8f, summary.trendChange!!, 0.001f)
    }

    @Test
    fun `a single weigh-in has no trend to report`() {
        val summary = summarise(listOf(WeighIn(today, 81.4f)), today)!!

        assertEquals(0L, summary.trendWeeks)
        assertNull(summary.trendChange)
        assertNull(summary.sevenDayAverage)
    }

    @Test
    fun `averages only the last seven days, and only once there are two`() {
        assertNull("weekly weigh-ins leave one in the window", summarise(weekly, today)!!.sevenDayAverage)

        val thisWeek = weekly + listOf(
            WeighIn(today.minusDays(2), 81.8f),
            WeighIn(today.minusDays(1), 81.6f),
        ).sortedBy { it.date }
        // The 81.4 from today plus the two above; the weigh-in a week back is out.
        assertEquals(81.6f, summarise(thisWeek.sortedBy { it.date }, today)!!.sevenDayAverage!!, 0.001f)
    }

    @Test
    fun `dates the latest weigh-in in the design's words`() {
        assertEquals("yesterday", summarise(listOf(WeighIn(today.minusDays(1), 81.4f)), today)!!.latestLabel)
        assertEquals("8 Aug", summarise(listOf(WeighIn(today.minusDays(7), 81.4f)), today)!!.latestLabel)
    }

    @Test
    fun `signs a change the way the design writes it`() {
        assertEquals("−2.8 kg", formatChange(-2.8f))
        assertEquals("+0.4 kg", formatChange(0.4f))
        assertEquals("0.0 kg", formatChange(0f))
    }
}
