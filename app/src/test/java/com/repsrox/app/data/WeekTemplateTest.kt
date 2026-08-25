package com.repsrox.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WeekTemplateTest {

    /** A Wednesday, so the week it belongs to is not the date itself. */
    private val wednesday = LocalDate.of(2026, 8, 12)
    private val monday = LocalDate.of(2026, 8, 10)

    private val squat = buildExercise("Back squat", sets = 5, reps = 5, kg = 120f)

    private val plan = WeekTemplate(
        id = "p1",
        name = "Hyrox build week",
        sessions = listOf(
            TemplateSession(DayOfWeek.MONDAY, "Lower push", SessionKind.STRENGTH, exercises = listOf(squat)),
            TemplateSession(DayOfWeek.TUESDAY, "Rest", SessionKind.REST, note = "Walk 6 km"),
            TemplateSession(DayOfWeek.THURSDAY, "8 km Zone 2", SessionKind.RUN, note = "42:00"),
        ),
    )

    private fun ids(): (Int) -> String {
        var next = 0
        return { "new-${next++}" }
    }

    @Test
    fun `a week starts on the monday it contains`() {
        assertEquals(monday, wednesday.weekStart())
        assertEquals(monday, monday.weekStart())
        assertEquals(monday, LocalDate.of(2026, 8, 16).weekStart())
        assertEquals(7, monday.weekDates().size)
    }

    @Test
    fun `laying a plan down puts each session on its own day`() {
        val sessions = plan.materialise(monday, ids())

        assertEquals(
            listOf(monday, monday.plusDays(1), monday.plusDays(3)),
            sessions.map { it.date },
        )
        assertEquals(listOf(squat), sessions.first().exercises)
    }

    @Test
    fun `applying replaces what was planned that week`() {
        val existing = listOf(
            PlannedSession("old", monday.plusDays(4), "Improvised", SessionKind.RUN),
        )

        val applied = applyTemplate(existing, plan, monday, ids())

        assertEquals(listOf("new-0", "new-1", "new-2"), applied.map { it.id })
    }

    @Test
    fun `applying leaves finished sessions and the days they sit on alone`() {
        val existing = listOf(
            PlannedSession("banked", monday, "Upper pull", SessionKind.STRENGTH, done = true),
        )

        val applied = applyTemplate(existing, plan, monday, ids())

        // The Monday it already holds is kept, and the plan's Monday is not doubled up.
        assertEquals(1, applied.count { it.date == monday })
        assertEquals("banked", applied.single { it.date == monday }.id)
        assertEquals(listOf(monday.plusDays(1), monday.plusDays(3)), applied.drop(1).map { it.date })
    }

    @Test
    fun `applying does not touch other weeks`() {
        val neighbour = PlannedSession("next", monday.plusWeeks(1), "Long Z2", SessionKind.RUN)

        val applied = applyTemplate(listOf(neighbour), plan, monday, ids())

        assertTrue(applied.contains(neighbour))
    }

    @Test
    fun `saving a week keeps its shape and drops what was finished`() {
        val week = listOf(
            PlannedSession("a", monday, "Lower push", SessionKind.STRENGTH, exercises = listOf(squat), done = true),
            PlannedSession("b", monday.plusDays(3), "8 km Zone 2", SessionKind.RUN, note = "42:00"),
            PlannedSession("c", monday.plusWeeks(1), "Not this week", SessionKind.RUN),
        )

        val saved = weekAsTemplate(week, monday, "Build week", "p9")

        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), saved.sessions.map { it.dayOfWeek })
        assertEquals(listOf(squat), saved.sessions.first().exercises)
    }

    @Test
    fun `a saved week survives a round trip through the store`() {
        assertEquals(listOf(plan), decodeTemplates(encodeTemplates(listOf(plan))))
    }

    @Test
    fun `a plan left with no readable session is dropped whole`() {
        val raw = """
            p1|Build|MONDAY|STRENGTH|Lower push||
            p2|Broken|NOTADAY|RUN|Ghost||
            p2|Broken|TUESDAY|SWIMMING|Ghost||
        """.trimIndent()

        assertEquals(listOf("p1"), decodeTemplates(raw).map { it.id })
    }

    @Test
    fun `a summary counts what the week asks for`() {
        assertEquals("2 sessions · 1 lift, 1 run", plan.summary())
    }
}
