package com.repsrox.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** The plan that repeats: what it prints onto the weeks ahead, and what it leaves alone. */
class RollingPlanTest {

    private val monday = LocalDate.of(2026, 9, 7)
    private val squat = buildExercise("Back squat", sets = 5, reps = 5, kg = 120f)

    private val plan = WeekTemplate(
        id = "p1",
        name = "Imported plan",
        sessions = listOf(
            TemplateSession(DayOfWeek.MONDAY, "Lower A", SessionKind.STRENGTH, exercises = listOf(squat)),
            TemplateSession(DayOfWeek.THURSDAY, "Upper A", SessionKind.STRENGTH, exercises = listOf(squat)),
        ),
    )

    private fun stored(date: LocalDate, id: String = "s1", done: Boolean = false) =
        PlannedSession(id, date, "Written down", SessionKind.STRENGTH, done = done)

    @Test
    fun `a plan prints onto every week ahead, not just this one`() {
        val projected = projectPlan(stored = emptyList(), rolling = plan, today = monday, weeks = 4)

        assertEquals(8, projected.size)
        assertEquals(
            listOf(monday, monday.plusDays(3), monday.plusWeeks(1), monday.plusWeeks(1).plusDays(3)),
            projected.take(4).map { it.date },
        )
        assertEquals(listOf("Lower A", "Upper A"), projected.take(2).map { it.name })
    }

    @Test
    fun `no plan in force leaves the week exactly as it was written`() {
        val written = listOf(stored(monday))
        assertEquals(written, projectPlan(written, rolling = null, today = monday))
        assertEquals(written, projectPlan(written, rolling = plan.copy(sessions = emptyList()), today = monday))
    }

    @Test
    fun `a week holding anything of its own is left whole to itself`() {
        val projected = projectPlan(listOf(stored(monday.plusDays(2))), plan, today = monday, weeks = 2)

        // This week is that one session and nothing else; next week still prints.
        assertEquals(listOf(monday.plusDays(2)), projected.filter { it.date < monday.plusWeeks(1) }.map { it.date })
        assertEquals(2, projected.count { it.date >= monday.plusWeeks(1) })
    }

    @Test
    fun `weeks behind you are never printed into`() {
        val lastWeek = monday.minusWeeks(1)
        val projected = projectPlan(emptyList(), plan, today = monday, weeks = 2)

        assertTrue(projected.none { it.date < monday })
        assertTrue(projected.none { it.date.weekStart() == lastWeek })
    }

    @Test
    fun `a printed session says which week it came from, a written one does not`() {
        val printed = projectPlan(emptyList(), plan, today = monday, weeks = 1).first()

        assertEquals(monday, rollingWeek(printed.id))
        assertNull(rollingWeek("a1"))
        assertNull(rollingWeek("rolling-not-a-date-0"))
    }

    @Test
    fun `editing a printed week writes it down first, so there is something to edit`() {
        val written = writeDownWeek(stored = emptyList(), rolling = plan, weekStart = monday)!!

        assertEquals(listOf(monday, monday.plusDays(3)), written.map { it.date })
        assertEquals(listOf("Lower A", "Upper A"), written.map { it.name })
        // The ids it lands under are the ones the week was being read by, so the
        // edit that triggered this still finds its own session afterwards.
        assertEquals(
            projectPlan(emptyList(), plan, monday, weeks = 1).map { it.id },
            written.map { it.id },
        )
    }

    @Test
    fun `a week already written down is left alone, and so is one with no plan`() {
        val own = listOf(stored(monday.plusDays(1)))

        assertNull(writeDownWeek(own, plan, monday))
        assertNull(writeDownWeek(emptyList(), rolling = null, weekStart = monday))
        assertNull(writeDownWeek(emptyList(), plan.copy(sessions = emptyList()), monday))
    }

    @Test
    fun `writing one week down leaves every other week to the plan`() {
        val written = writeDownWeek(emptyList(), plan, monday)!!
        val projected = projectPlan(written, plan, today = monday, weeks = 3)

        // This week is now its own; the two ahead of it are still the plan's.
        assertEquals(2, projected.count { it.date.weekStart() == monday })
        assertEquals(2, projected.count { it.date.weekStart() == monday.plusWeeks(1) })
        assertTrue(projected.filter { it.date.weekStart() == monday }.none { rollingWeek(it.id) != monday })
    }

    @Test
    fun `a finished week keeps its record when the plan is still in force`() {
        val done = listOf(stored(monday, id = "banked", done = true))
        val projected = projectPlan(done, plan, today = monday, weeks = 2)

        assertEquals(listOf("banked"), projected.filter { it.date.weekStart() == monday }.map { it.id })
        assertTrue(projected.single { it.id == "banked" }.done)
    }

    @Test
    fun `a document becomes a plan keyed by the day of the week, not the date`() {
        val parsed = parsePlan(
            """
            ## Sessions

            ### 2026-09-07 · Lower A — Quad focus
            | Exercise | Sets | Reps | Weight |
            | --- | --- | --- | --- |
            | Back squat | 4 | 10 | 60 |

            ### 2026-09-10 · Upper A
            | Exercise | Sets | Reps | Weight |
            | --- | --- | --- | --- |
            | Bench press | 3 | 8 | 50 |
            """.trimIndent(),
        )

        val template = parsed.asWeekTemplate(id = "t1", name = "Imported plan")!!

        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), template.sessions.map { it.dayOfWeek })
        assertEquals(listOf("Lower A — Quad focus", "Upper A"), template.sessions.map { it.name })
        assertEquals(listOf("Back squat"), template.sessions.first().exercises.map { it.name })
    }

    @Test
    fun `a session heading with only a date still names its day`() {
        val parsed = parsePlan(
            """
            ## Sessions

            ### 2026-09-08
            | Exercise | Sets | Reps | Weight |
            | --- | --- | --- | --- |
            | Deadlift | 3 | 5 | 100 |
            """.trimIndent(),
        )

        assertTrue(parsed.names.isEmpty())
        assertEquals("Tuesday", parsed.asWeekTemplate("t1", "Imported plan")!!.sessions.single().name)
    }

    @Test
    fun `a document longer than a week keeps the first of a repeated day`() {
        val parsed = parsePlan(
            """
            ## Sessions

            ### 2026-09-07 · First Monday
            | Exercise | Sets | Reps | Weight |
            | --- | --- | --- | --- |
            | Back squat | 5 | 5 | 120 |

            ### 2026-09-14 · Second Monday
            | Exercise | Sets | Reps | Weight |
            | --- | --- | --- | --- |
            | Front squat | 5 | 5 | 90 |
            """.trimIndent(),
        )

        val template = parsed.asWeekTemplate("t1", "Imported plan")!!
        assertEquals("First Monday", template.sessions.single().name)
    }

    @Test
    fun `a document with no sessions is no plan at all`() {
        assertNull(parsePlan("## Sessions\n\nNothing planned.").asWeekTemplate("t1", "Imported plan"))
    }

    // ── The week already written down ───────────────────────────────────────

    @Test
    fun `a new plan re-shapes the week under way, not just the weeks ahead`() {
        // The week the app is looked at in is always written down, so without this
        // the home screen would keep showing the session the import replaced.
        val seeded = listOf(
            PlannedSession("seed-0", monday, "Old Monday", SessionKind.STRENGTH),
            PlannedSession("seed-1", monday.plusDays(2), "Old Wednesday", SessionKind.RUN),
        )

        val written = applyPlanToWrittenWeeks(seeded, plan, today = monday) { "new-$it" }

        assertEquals(listOf(monday, monday.plusDays(3)), written.map { it.date })
        assertEquals(listOf("Lower A", "Upper A"), written.map { it.name })
    }

    @Test
    fun `a banked session survives the plan being re-laid over its week`() {
        val banked = PlannedSession("s1", monday, "Old Monday", SessionKind.STRENGTH, done = true)

        val written = applyPlanToWrittenWeeks(listOf(banked), plan, today = monday) { "new-$it" }

        // Monday is spoken for by what was actually done; Thursday still lands.
        assertEquals(banked, written.first())
        assertEquals(listOf(monday, monday.plusDays(3)), written.map { it.date })
    }

    @Test
    fun `weeks behind you are left as they were written`() {
        val lastWeek = listOf(stored(monday.minusWeeks(1), id = "old"))

        assertEquals(lastWeek, applyPlanToWrittenWeeks(lastWeek, plan, today = monday) { "new-$it" })
    }

    @Test
    fun `a written-down week further ahead takes the plan too`() {
        val nextWeek = listOf(stored(monday.plusWeeks(1).plusDays(1), id = "ahead"))

        val written = applyPlanToWrittenWeeks(nextWeek, plan, today = monday) { "new-$it" }

        assertEquals(
            listOf(monday.plusWeeks(1), monday.plusWeeks(1).plusDays(3)),
            written.map { it.date },
        )
    }

    @Test
    fun `a week holding nothing of its own is left to the plan to print`() {
        assertEquals(emptyList<PlannedSession>(), applyPlanToWrittenWeeks(emptyList(), plan, monday) { "new-$it" })
    }
}
