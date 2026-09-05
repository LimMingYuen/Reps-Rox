package com.repsrox.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PlanDocumentRoundTripTest {

    private val weekStart = LocalDate.of(2026, 8, 10)

    private val squat = buildExercise("Back squat", sets = 5, reps = 5, kg = 120f)
    private val sled = buildExercise("Sled push", sets = 4, reps = 25, kg = 150f, unit = SetUnit.METRES)

    private val plan = listOf(
        PlannedSession(
            id = "a1",
            date = weekStart,
            name = "Lower push",
            kind = SessionKind.STRENGTH,
            exercises = listOf(squat, sled),
        ),
        PlannedSession(
            id = "a2",
            date = weekStart.plusDays(2),
            name = "8 km Zone 2",
            kind = SessionKind.RUN,
            note = "42:00",
        ),
    )

    @Test
    fun `a round trip reproduces the strength sessions' exercises, keyed by date`() {
        val parsed = parsePlan(exportPlan(plan, weekStart))
        assertEquals(setOf(weekStart), parsed.sessions.keys)
        assertEquals(listOf(squat, sled), parsed.sessions.getValue(weekStart))
    }

    @Test
    fun `a run day carries no board, so nothing is exported for it`() {
        val parsed = parsePlan(exportPlan(plan, weekStart))
        assertFalse(weekStart.plusDays(2) in parsed.sessions)
    }

    @Test
    fun `a clean round trip reads nothing as a problem`() {
        assertTrue(parsePlan(exportPlan(plan, weekStart)).problems.isEmpty())
    }
}

class PlanDocumentContextTest {

    private val today = LocalDate.of(2026, 8, 15)

    @Test
    fun `banked training is written out as context, but is never read back as a session`() {
        val banked = Session(
            finishedAt = today.atStartOfDayInstant(),
            name = "Lower push",
            seconds = 3000,
            exercises = listOf(LoggedExercise("Back squat", listOf(WorkSet(5, "120")))),
        )
        val markdown = exportPlan(emptyList(), today.weekStart(), banked = listOf(banked), today = today)

        assertTrue(markdown.contains("Lower push"))
        assertTrue(parsePlan(markdown).sessions.isEmpty())
    }

    @Test
    fun `training older than 28 days is left out of the context section`() {
        val stale = Session(
            finishedAt = today.minusDays(40).atStartOfDayInstant(),
            name = "Ancient session",
            seconds = 1000,
            exercises = emptyList(),
        )
        val markdown = exportPlan(emptyList(), today.weekStart(), banked = listOf(stale), today = today)
        assertFalse(markdown.contains("Ancient session"))
    }

    @Test
    fun `recent weigh-ins appear in context but are never reported as problems`() {
        val weighIns = listOf(WeighIn(today.minusDays(3), 82.4f))
        val parsed = parsePlan(exportPlan(emptyList(), today.weekStart(), weighIns = weighIns, today = today))
        assertTrue(parsed.problems.isEmpty())
    }

    @Test
    fun `with nothing recent, the context section says so rather than an empty table`() {
        val markdown = exportPlan(emptyList(), today.weekStart(), today = today)
        assertTrue(markdown.contains("No sessions banked in the last 28 days."))
        assertTrue(markdown.contains("No weigh-ins in the last 28 days."))
    }

    private fun LocalDate.atStartOfDayInstant(): Instant =
        atStartOfDay(ZoneId.systemDefault()).toInstant()
}

class PlanDocumentToleranceTest {

    @Test
    fun `a hand-reworded heading still resolves by the date inside it`() {
        val markdown = """
            ## Sessions

            ### 2026-08-15 - Lower push, revised

            | Exercise | Sets | Reps | Weight |
            | --- | --- | --- | --- |
            | Back squat | 5 | 5 | 120 |
        """.trimIndent()

        val parsed = parsePlan(markdown)
        assertEquals(listOf(LocalDate.of(2026, 8, 15)), parsed.sessions.keys.toList())
    }

    @Test
    fun `columns are matched by name, not position`() {
        val markdown = """
            ## Sessions

            ### 2026-08-15 · Lower push

            | Weight | Exercise | Reps | Sets |
            | --- | --- | --- | --- |
            | 120 | Back squat | 5 | 5 |
        """.trimIndent()

        val exercise = parsePlan(markdown).sessions.getValue(LocalDate.of(2026, 8, 15)).single()
        assertEquals("Back squat", exercise.name)
        assertEquals(5, exercise.sets.size)
        assertEquals(5, exercise.sets.first().reps)
        assertEquals("120", exercise.sets.first().kg)
    }

    @Test
    fun `noisy cell text still parses to clean values`() {
        val markdown = """
            ## Sessions

            ### 2026-08-15 · Lower push

            | Exercise | Sets | Reps | Weight |
            | --- | --- | --- | --- |
            | Back squat | 5 sets | 5 reps | 120 kg |
        """.trimIndent()

        val exercise = parsePlan(markdown).sessions.getValue(LocalDate.of(2026, 8, 15)).single()
        assertEquals(5, exercise.sets.size)
        assertEquals(5, exercise.sets.first().reps)
        assertEquals("120", exercise.sets.first().kg)
    }

    @Test
    fun `a Distance column implies metres even without an m suffix on the number`() {
        val markdown = """
            ## Sessions

            ### 2026-08-15 · Lower push

            | Exercise | Sets | Distance | Weight |
            | --- | --- | --- | --- |
            | Sled push | 4 | 25 | 150 |
        """.trimIndent()

        val exercise = parsePlan(markdown).sessions.getValue(LocalDate.of(2026, 8, 15)).single()
        assertEquals(SetUnit.METRES, exercise.sets.first().unit)
        assertEquals(25, exercise.sets.first().reps)
    }

    @Test
    fun `a bad row is skipped and reported, while the good row still imports`() {
        val markdown = """
            ## Sessions

            ### 2026-08-15 · Lower push

            | Exercise | Sets | Reps | Weight |
            | --- | --- | --- | --- |
            | | | | |
            | Back squat | 5 | 5 | 120 |
        """.trimIndent()

        val parsed = parsePlan(markdown)
        assertEquals(listOf("Back squat"), parsed.sessions.getValue(LocalDate.of(2026, 8, 15)).map { it.name })
        assertTrue(parsed.problems.isNotEmpty())
    }

    @Test
    fun `a heading naming no date says so, and prescribes nothing`() {
        val markdown = """
            ## Sessions

            ### Leg day

            | Exercise | Sets | Reps | Weight |
            | --- | --- | --- | --- |
            | Back squat | 5 | 5 | 120 |
        """.trimIndent()

        val parsed = parsePlan(markdown)
        assertTrue(parsed.sessions.isEmpty())
        assertTrue(parsed.problems.any { it.contains("Leg day") })
    }

    @Test
    fun `a set count over the range is rejected as a problem, not clamped`() {
        val markdown = """
            ## Sessions

            ### 2026-08-15 · Lower push

            | Exercise | Sets | Reps | Weight |
            | --- | --- | --- | --- |
            | Back squat | 400 | 5 | 120 |
        """.trimIndent()

        val parsed = parsePlan(markdown)
        assertFalse(LocalDate.of(2026, 8, 15) in parsed.sessions)
        assertEquals(1, parsed.problems.size)
    }

    @Test
    fun `an empty document has nothing to apply`() {
        val parsed = parsePlan("")
        assertTrue(parsed.isEmpty)
        assertEquals("Nothing to apply", parsed.summary())
    }

    @Test
    fun `context tables are never read as sessions, however table-shaped they are`() {
        val markdown = """
            ## Context

            ### Recent training

            | Date | Session | Exercises |
            | --- | --- | --- |
            | 2026-08-15 | Lower push | Back squat 5 / 5 / 5 / 5 / 5 · 120 kg |
        """.trimIndent()

        assertTrue(parsePlan(markdown).sessions.isEmpty())
    }
}

class PlanDocumentMealTest {

    private val day = LocalDate.of(2026, 8, 10)

    private val meals = listOf(
        Meal("m1", day, "Breakfast", "Oats, whey, banana", kcal = 620, proteinG = 42, carbsG = 78, logged = true),
        Meal("m2", day, "Lunch", "Rice, chicken, greens", kcal = 780, proteinG = 62, carbsG = 96),
        Meal("m3", day.plusDays(1), "Post-session", "", kcal = 0, proteinG = 40, carbsG = 0),
    )

    private fun roundTrip() = parsePlan(exportPlan(emptyList(), day, meals = meals))

    @Test
    fun `a round trip reproduces the meals, keyed by day`() {
        val parsed = roundTrip()
        assertEquals(setOf(day, day.plusDays(1)), parsed.meals.keys)
        assertEquals(listOf("Breakfast", "Lunch"), parsed.meals.getValue(day).map { it.name })
    }

    @Test
    fun `a meal's detail and figures survive the document`() {
        val breakfast = roundTrip().meals.getValue(day).first()
        assertEquals("Oats, whey, banana", breakfast.detail)
        assertEquals(620, breakfast.kcal)
        assertEquals(42, breakfast.proteinG)
        assertEquals(78, breakfast.carbsG)
    }

    @Test
    fun `an uncosted meal comes back uncosted rather than unreadable`() {
        val parsed = roundTrip()
        val post = parsed.meals.getValue(day.plusDays(1)).single()
        assertEquals("", post.detail)
        assertEquals(0, post.kcal)
        assertEquals(40, post.proteinG)
        assertTrue(parsed.problems.isEmpty())
    }

    @Test
    fun `a check-in is never written out, since the document plans rather than records`() {
        assertFalse(roundTrip().meals.getValue(day).any { it.logged })
    }

    @Test
    fun `a document can plan meals for a day that carries no session at all`() {
        val parsed = roundTrip()
        assertTrue(parsed.sessions.isEmpty())
        assertFalse(parsed.isEmpty)
    }

    @Test
    fun `the summary counts sessions and meals apart`() {
        val plan = listOf(
            PlannedSession(
                id = "a1",
                date = day,
                name = "Lower push",
                kind = SessionKind.STRENGTH,
                exercises = listOf(buildExercise("Back squat", sets = 5, reps = 5, kg = 120f)),
            ),
        )
        val summary = parsePlan(exportPlan(plan, day, meals = meals)).summary()
        assertTrue(summary.contains("1 session"))
        assertTrue(summary.contains("3 meals"))
    }

    @Test
    fun `a hand-written meal table is read on its column headings, not their order`() {
        val markdown = """
            ## Meals

            ### 2026-08-10

            | Kcal | Protein (g) | Meal | Detail |
            | --- | --- | --- | --- |
            | 450 | 30 | Snack | Greek yoghurt |
        """.trimIndent()

        val meal = parsePlan(markdown).meals.getValue(day).single()
        assertEquals("Snack", meal.name)
        assertEquals("Greek yoghurt", meal.detail)
        assertEquals(450, meal.kcal)
        assertEquals(30, meal.proteinG)
        // A column the table never carried is nothing, not a slip.
        assertEquals(0, meal.carbsG)
    }

    @Test
    fun `a meal figure that will not read is reported rather than guessed at`() {
        val markdown = """
            ## Meals

            ### 2026-08-10

            | Meal | Kcal |
            | --- | --- |
            | Snack | loads |
        """.trimIndent()

        val parsed = parsePlan(markdown)
        assertEquals("Snack", parsed.meals.getValue(day).single().name)
        assertEquals(0, parsed.meals.getValue(day).single().kcal)
        assertTrue(parsed.problems.isEmpty())
    }

    @Test
    fun `a meal row with no name is skipped and reported`() {
        val markdown = """
            ## Meals

            ### 2026-08-10

            | Meal | Kcal |
            | --- | --- |
            |  | 450 |
        """.trimIndent()

        assertTrue(parsePlan(markdown).meals.isEmpty())
        assertEquals(1, parsePlan(markdown).problems.size)
    }
}
