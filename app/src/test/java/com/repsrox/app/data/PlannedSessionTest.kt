package com.repsrox.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PlanCodecTest {

    private val date = LocalDate.of(2026, 8, 15)

    private val squat = buildExercise("Back squat", sets = 5, reps = 5, kg = 120f)
    private val burpees = buildExercise("Burpees", sets = 3, reps = 12, kg = 0f)

    private val session = PlannedSession(
        id = "a1",
        date = date,
        name = "Lower push + sled finisher",
        kind = SessionKind.STRENGTH,
        exercises = listOf(squat, burpees),
    )

    @Test
    fun `round trips a plan`() {
        val plan = listOf(
            session,
            PlannedSession("b2", date.plusDays(1), "8 km Zone 2", SessionKind.RUN, note = "42:18", done = true),
        )
        assertEquals(plan, decodePlan(encodePlan(plan)))
    }

    @Test
    fun `round trips bodyweight work, which carries no load`() {
        assertEquals(listOf(burpees), decodePlan(encodePlan(listOf(session)))[0].exercises.drop(1))
    }

    @Test
    fun `keeps the readable records and drops the rest`() {
        val raw = listOf(
            encodePlan(listOf(session)),
            "b2|not-a-date|RUN|0|Long Z2||",
            "c3|2026-08-17|SWIMMING|0|Intervals||",
            "|2026-08-18|RUN|0|Unidentified||",
            "d4|2026-08-19|RACE|0|Full race sim|8 stations|",
        ).joinToString("\n")

        assertEquals(listOf("a1", "d4"), decodePlan(raw).map { it.id })
    }

    @Test
    fun `drops an exercise whose sets cannot be read`() {
        val raw = "a1|2026-08-15|STRENGTH|0|Lower push||Back squat~5 × 5~5x120;Ghost~3 × 3~"
        assertEquals(listOf("Back squat"), decodePlan(raw).single().exercises.map { it.name })
    }

    @Test
    fun `strips the characters the record format claims`() {
        val name = sanitise("Sled push | heavy ~ 3;4")
        assertEquals("Sled push  heavy  34", name)

        val plan = listOf(session.copy(name = name))
        assertEquals(name, decodePlan(encodePlan(plan)).single().name)
    }

    @Test
    fun `round trips a metres set`() {
        val sled = buildExercise("Sled push", sets = 4, reps = 25, kg = 150f, unit = SetUnit.METRES)
        val plan = listOf(session.copy(exercises = listOf(sled)))
        val decoded = decodePlan(encodePlan(plan)).single().exercises.single()
        assertEquals(SetUnit.METRES, decoded.sets.first().unit)
        assertEquals(sled, decoded)
    }

    @Test
    fun `a reps set still decodes from an old two-field record`() {
        // On-disk records written before metres existed carry only reps and kg.
        val raw = "a1|2026-08-15|STRENGTH|0|Lower push||Back squat~5 × 5~5x120"
        assertEquals(SetUnit.REPS, decodePlan(raw).single().exercises.single().sets.first().unit)
    }
}

class PlannedSessionTest {

    private val today = LocalDate.of(2026, 8, 15)

    private val session = PlannedSession(
        id = "a1",
        date = today,
        name = "Lower push",
        kind = SessionKind.STRENGTH,
        exercises = listOf(buildExercise("Back squat", sets = 5, reps = 5, kg = 120f)),
    )

    @Test
    fun `counts sets and tonnage across the session`() {
        assertEquals(5, session.plannedSets)
        assertEquals(3000f, session.volumeKg, 0.01f)
        assertEquals("3.0 t", formatTonnes(session.volumeKg))
    }

    @Test
    fun `a session finished is done, whatever day it falls on`() {
        assertEquals(DayStatus.DONE, session.copy(done = true).status(today))
        assertEquals(DayStatus.DONE, session.copy(date = today.plusDays(2), done = true).status(today))
    }

    @Test
    fun `a session left unfinished stays planned rather than claiming it was done`() {
        assertEquals(DayStatus.TODAY, session.status(today))
        assertEquals(DayStatus.PLANNED, session.copy(date = today.minusDays(3)).status(today))
        assertEquals(DayStatus.PLANNED, session.copy(date = today.plusDays(1)).status(today))
    }

    @Test
    fun `rest is rest, finished or not`() {
        val rest = session.copy(kind = SessionKind.REST, exercises = emptyList(), done = true)
        assertEquals(DayStatus.REST, rest.status(today))
    }

    @Test
    fun `meta counts what the session holds, and prefers the note when there is one`() {
        assertEquals("Today · 1 exercise · 5 sets", session.meta(today))
        assertEquals("Today · 8 km easy", session.copy(note = "8 km easy").meta(today))
    }

    @Test
    fun `a target names its load only when it carries one`() {
        assertEquals("5 × 5 · 120 kg", buildExercise("Back squat", 5, 5, 120f).target)
        assertEquals("3 × 12", buildExercise("Burpees", 3, 12, 0f).target)
        assertEquals("4 × 8 · 22.5 kg", buildExercise("Row", 4, 8, 22.5f).target)
    }

    @Test
    fun `a metres target states its distance, the way a carry is actually written`() {
        assertEquals(
            "4 × 25 m · 150 kg",
            buildExercise("Sled push", 4, 25, 150f, SetUnit.METRES).target,
        )
        assertEquals("4 × 25 m", buildExercise("Sprint", 4, 25, 0f, SetUnit.METRES).target)
    }

    @Test
    fun `a log line calls a shared load once and drops it when there is none`() {
        assertEquals("5 / 5 / 5 / 5 / 5 · 120 kg", buildExercise("Back squat", 5, 5, 120f).logLine())
        assertEquals("12 / 12 / 12", buildExercise("Burpees", 3, 12, 0f).logLine())
    }

    @Test
    fun `a metres log line calls the distance covered, not a rep count`() {
        assertEquals(
            "4 × 25 m · 150 kg",
            buildExercise("Sled push", 4, 25, 150f, SetUnit.METRES).logLine(),
        )
    }

    @Test
    fun `a metres set carries no tonnage`() {
        val carry = PlannedSession(
            id = "a1",
            date = today,
            name = "Loaded carries",
            kind = SessionKind.STRENGTH,
            exercises = listOf(buildExercise("Farmers carry", 4, 40, 32f, SetUnit.METRES)),
        )
        assertEquals(0f, carry.volumeKg, 0.01f)
    }
}
