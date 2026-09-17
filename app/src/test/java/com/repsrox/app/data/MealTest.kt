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
