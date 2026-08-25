package com.repsrox.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val FINISHED_AT: Instant = Instant.ofEpochSecond(1_755_302_400)

private fun session(
    name: String = "Lower push + sled finisher",
    seconds: Int = 3492,
    exercises: List<LoggedExercise> = listOf(
        LoggedExercise("Back squat", List(5) { WorkSet(5, "120") }),
        LoggedExercise("Sled push", List(4) { WorkSet(25, "150", SetUnit.METRES) }),
    ),
) = Session(FINISHED_AT, name, seconds, exercises)

class SessionLogCodecTest {

    @Test
    fun `round trips a session`() {
        assertEquals(listOf(session()), decodeSessions(encodeSessions(listOf(session()))))
    }

    @Test
    fun `keeps the unit that says a figure is metres`() {
        val decoded = decodeSessions(encodeSessions(listOf(session())))
        assertEquals(SetUnit.METRES, decoded.single().exercises[1].sets.first().unit)
    }

    @Test
    fun `keeps the readable records and drops the rest`() {
        val raw = """
            1755302400|3492|Lower push|Back squat:5*120
            not-a-time|3492|Lower push|Back squat:5*120
            1755216000|nope|Lower push|Back squat:5*120
            1755129600|1800|Upper pull|
        """.trimIndent()

        assertEquals(listOf("Lower push"), decodeSessions(raw).map { it.name })
    }

    @Test
    fun `drops the sets it cannot read but keeps the session`() {
        val decoded = decodeSessions("1755302400|3492|Lower push|Back squat:5*120,x*120,5*heavy")
        assertEquals(listOf(WorkSet(5, "120")), decoded.single().exercises.single().sets)
    }

    @Test
    fun `decodes an emptied log as empty, not as a bad record`() {
        assertEquals(emptyList<Session>(), decodeSessions(""))
    }

    @Test
    fun `returns sessions newest first however they were stored`() {
        val raw = """
            1755129600|1800|Older|Back squat:5*120
            1755302400|3492|Newer|Back squat:5*120
        """.trimIndent()

        assertEquals(listOf("Newer", "Older"), decodeSessions(raw).map { it.name })
    }

    @Test
    fun `strips separators out of a name rather than letting it break the record`() {
        val decoded = decodeSessions(encodeSessions(listOf(session(name = "Legs|and:sled"))))
        assertEquals("Legsandsled", decoded.single().name)
    }
}

class SessionFiguresTest {

    @Test
    fun `counts every banked set`() {
        assertEquals(9, session().totalSets)
    }

    @Test
    fun `banks tonnage from the loaded sets only`() {
        // 5 × 5 × 120 kg; the sled's four 25 m lengths are a distance, not reps.
        assertEquals(3000f, session().volumeKg, 0.01f)
    }

    @Test
    fun `reads the heaviest loaded set, never a carry`() {
        val heavy = session(
            exercises = listOf(
                LoggedExercise("Back squat", listOf(WorkSet(5, "120"), WorkSet(3, "140"))),
                LoggedExercise("Sled push", listOf(WorkSet(25, "150", SetUnit.METRES))),
            ),
        )
        assertEquals(TopSet("Back squat", WorkSet(3, "140")), heavy.topSet)
    }

    @Test
    fun `has no top set when nothing loaded was banked`() {
        val carriesOnly = session(
            exercises = listOf(
                LoggedExercise("Sled push", listOf(WorkSet(25, "150", SetUnit.METRES))),
            ),
        )
        assertNull(carriesOnly.topSet)
    }

    @Test
    fun `shows tonnes above a tonne and kilos below it`() {
        assertEquals("7.8t", formatVolume(7820f))
        assertEquals("840 kg", formatVolume(840f))
    }
}

class SessionDetailTest {

    @Test
    fun `spells out the reps when the weight held`() {
        val exercise = LoggedExercise("Back squat", List(3) { WorkSet(5, "120") })
        assertEquals("5 / 5 / 5 · 120 kg", exercise.detail())
    }

    @Test
    fun `counts the lengths when the figure is metres`() {
        val exercise = LoggedExercise("Sled push", List(4) { WorkSet(25, "150", SetUnit.METRES) })
        assertEquals("4 × 25 m · 150 kg", exercise.detail())
    }

    @Test
    fun `pairs reps with weight when the load moved between sets`() {
        val exercise = LoggedExercise("Back squat", listOf(WorkSet(5, "120"), WorkSet(3, "140")))
        assertEquals("5×120 / 3×140", exercise.detail())
    }
}

class SessionDayLabelTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    /** Saturday, and the day the design's week calls today. */
    private val today: LocalDate = LocalDate.of(2026, 8, 15)

    private fun bankedOn(date: LocalDate) =
        session().copy(finishedAt = date.atStartOfDay(zone).toInstant())

    private fun labelFor(date: LocalDate) = bankedOn(date).dayLabel(today, zone)

    @Test
    fun `names today and yesterday rather than dating them`() {
        assertEquals("today", labelFor(today))
        assertEquals("yesterday", labelFor(today.minusDays(1)))
    }

    @Test
    fun `gives the weekday for the rest of the week`() {
        assertEquals("Wed", labelFor(today.minusDays(3)))
        assertEquals("Sun", labelFor(today.minusDays(6)))
    }

    @Test
    fun `dates anything a weekday would be ambiguous for`() {
        assertEquals("8 Aug", labelFor(today.minusDays(7)))
        assertEquals("5 Aug", labelFor(today.minusDays(10)))
    }

    @Test
    fun `reads the day off the zone it was banked in`() {
        assertEquals(today, bankedOn(today).day(zone))
    }
}
