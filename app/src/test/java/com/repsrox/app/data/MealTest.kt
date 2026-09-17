package com.repsrox.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

private val DAY: LocalDate = LocalDate.of(2026, 8, 15)

private fun meal(
    id: String,
    name: String,
    kcal: Int = 0,
    protein: Int = 0,
    carbs: Int = 0,
    logged: Boolean = false,
    date: LocalDate = DAY,
) = Meal(id, date, name, "", kcal, protein, carbs, logged)

class FuelSummaryTest {

    private val day = listOf(
        meal("1", "Breakfast", kcal = 620, protein = 42, carbs = 78, logged = true),
        meal("2", "Lunch", kcal = 780, protein = 62, carbs = 96, logged = true),
        meal("3", "Dinner", kcal = 720, protein = 48, carbs = 74),
    )

    @Test
    fun `only meals checked in count towards the macros`() {
        val summary = summariseFuel(day)
        assertEquals(1400, summary.loggedKcal)
        assertEquals(104, summary.loggedProteinG)
        assertEquals(174, summary.loggedCarbsG)
    }

    @Test
    fun `the whole day is still carried, so what is left to eat can be read off it`() {
        val summary = summariseFuel(day)
        assertEquals(2120, summary.plannedKcal)
        assertEquals(2, summary.logged)
        assertEquals(3, summary.meals)
    }

    @Test
    fun `a day that trains is aiming higher than one that does not`() {
        assertEquals(BASE_TARGET_KCAL, targetKcal(training = false))
        assertEquals(BASE_TARGET_KCAL + TRAINING_KCAL, targetKcal(training = true))
    }

    @Test
    fun `the calorie row reads what was banked against what the day is aiming at`() {
        val calories = summariseFuel(day, targetKcal(training = true)).macros.first()
        assertEquals("Calories", calories.label)
        assertEquals("1,400 / 2,540", calories.value)
        assertEquals(55, calories.percent)
    }

    @Test
    fun `an empty day says so rather than reporting on nothing`() {
        val summary = summariseFuel(emptyList())
        assertEquals("Nothing planned", summary.meta())
        assertEquals(0, summary.macros.first().percent)
    }
}

class MealDayTest {

    private val tomorrow = DAY.plusDays(1)

    private val current = listOf(
        meal("1", "Breakfast", kcal = 620, logged = true),
        meal("2", "Lunch", kcal = 780),
        meal("3", "Breakfast", kcal = 500, date = tomorrow),
    )

    @Test
    fun `an imported day replaces what that day held`() {
        val applied = applyMealDays(current, mapOf(DAY to listOf(meal("x", "Brunch", kcal = 900))))
        assertEquals(listOf("Brunch"), applied.filter { it.date == DAY }.map { it.name })
    }

    @Test
    fun `a day the document leaves out is left alone`() {
        val applied = applyMealDays(current, mapOf(DAY to listOf(meal("x", "Brunch"))))
        assertEquals(listOf("Breakfast"), applied.filter { it.date == tomorrow }.map { it.name })
    }

    @Test
    fun `a check-in survives a re-import of the same day`() {
        val applied = applyMealDays(
            current,
            mapOf(DAY to listOf(meal("x", "Breakfast", kcal = 640), meal("y", "Lunch", kcal = 800))),
        )
        val byName = applied.filter { it.date == DAY }.associateBy { it.name }
        assertTrue(byName.getValue("Breakfast").logged)
        // Lunch was never checked in, so it comes back as the plan it is.
        assertTrue(!byName.getValue("Lunch").logged)
        assertEquals(640, byName.getValue("Breakfast").kcal)
    }

    @Test
    fun `importing nothing changes nothing`() {
        assertEquals(current, applyMealDays(current, emptyMap()))
    }
}

class MealRecordTest {

    @Test
    fun `a log survives a round trip through the record format`() {
        val log = listOf(
            meal("1", "Breakfast", kcal = 620, protein = 42, carbs = 78, logged = true),
            meal("2", "Post-session", date = DAY.plusDays(1)),
        )
        assertEquals(log, decodeMeals(encodeMeals(log)))
    }

    @Test
    fun `a line that cannot be read is skipped rather than losing the log`() {
        val good = meal("1", "Breakfast", kcal = 620)
        val raw = encodeMeals(listOf(good)) + "\nnonsense\n|2026-08-15|No id|||||0"
        assertEquals(listOf(good), decodeMeals(raw))
    }

    @Test
    fun `a meal with no name is not a meal`() {
        assertNull(decodeMeals("1|2026-08-15||detail|0|0|0|0").firstOrNull())
    }
}

class RollingMealsTest {

    // Thursday; the week runs Monday 14th to Sunday 20th.
    private val today: LocalDate = LocalDate.of(2026, 9, 17)

    /** Monday 7th to Sunday 13th — a document written against last week. */
    private val lastWeek = (7..13).associate { day ->
        val date = LocalDate.of(2026, 9, day)
        date to listOf(meal("$date#0", "Breakfast $date", date = date, logged = true))
    }
    private val rolling = mealWeek(lastWeek)

    @Test
    fun `the week prints onto every day ahead by weekday, unlogged`() {
        val meals = projectMeals(emptyList(), rolling, emptySet(), today)

        val nextThursday = meals.single { it.date == today.plusWeeks(1) }
        assertEquals("Breakfast 2026-09-10", nextThursday.name)
        assertEquals(rollingMealId(today.plusWeeks(1), 0), nextThursday.id)
        assertTrue(meals.none { it.logged })
        assertEquals(today, meals.first().date)
    }

    @Test
    fun `a day with meals of its own, or written down, is left alone`() {
        val own = meal("mine", "Brunch", date = today)
        val meals = projectMeals(listOf(own), rolling, setOf(today.plusDays(1)), today)

        assertEquals(listOf(own), meals.filter { it.date == today })
        assertTrue(meals.none { it.date == today.plusDays(1) })
    }

    @Test
    fun `a printed meal's id names its day`() {
        assertEquals(today, rollingMealDay(rollingMealId(today, 3)))
        assertNull(rollingMealDay("mine"))
    }

    @Test
    fun `writing a day down stores what was printed, once`() {
        val written = writeDownMealDay(emptyList(), rolling, emptySet(), today, today)!!

        assertEquals(rollingMealId(today, 0), written.single().id)
        assertNull(writeDownMealDay(written, rolling, emptySet(), today, today))
        assertNull(writeDownMealDay(emptyList(), rolling, setOf(today), today, today))
        assertNull(writeDownMealDay(emptyList(), rolling, emptySet(), today.minusDays(1), today))
    }

    @Test
    fun `a new document lands on days ahead already written down`() {
        val tomorrow = today.plusDays(1)
        val stored = listOf(meal("old", "Old", date = tomorrow), meal("past", "Past", date = today.minusDays(1)))

        val days = importMealDays(lastWeek, stored, setOf(today), today)

        assertEquals("Breakfast 2026-09-11", days.getValue(tomorrow).single().name)
        assertEquals("Breakfast 2026-09-10", days.getValue(today).single().name)
        assertNull(days[today.minusDays(1)])
        assertEquals(lastWeek.getValue(LocalDate.of(2026, 9, 7)), days[LocalDate.of(2026, 9, 7)])
    }
}
