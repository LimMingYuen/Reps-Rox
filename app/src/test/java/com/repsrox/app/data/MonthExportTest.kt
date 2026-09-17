package com.repsrox.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

private val AUGUST: YearMonth = YearMonth.of(2026, 8)
private val UTC = ZoneOffset.UTC

private fun at(date: LocalDate) = date.atTime(18, 0).toInstant(UTC)

class MonthExportTest {

    private val squat = Session(
        finishedAt = at(LocalDate.of(2026, 8, 15)),
        name = "Lower push",
        seconds = 3492,
        exercises = listOf(
            LoggedExercise("Back squat", listOf(WorkSet(5, "120"), WorkSet(5, "122.5"))),
            LoggedExercise("Sled push", listOf(WorkSet(25, "150", SetUnit.METRES))),
        ),
    )

    @Test
    fun `training writes a row a set, and only the month asked for`() {
        val july = squat.copy(finishedAt = at(LocalDate.of(2026, 7, 31)))
        val file = trainingCsv(AUGUST, listOf(squat, july), UTC)

        assertEquals("repsrox-training-2026-08.csv", file.name)
        assertEquals(3, file.rows)
        assertEquals(
            listOf(
                "Date,Session,Duration,Exercise,Set,Amount,Unit,Weight kg",
                "2026-08-15,Lower push,58:12,Back squat,1,5,reps,120",
                "2026-08-15,Lower push,58:12,Back squat,2,5,reps,122.5",
                "2026-08-15,Lower push,58:12,Sled push,1,25,m,150",
            ),
            file.text.trimEnd().split("\r\n"),
        )
    }

    @Test
    fun `meals carry whether they were eaten`() {
        val meals = listOf(
            Meal("m1", LocalDate.of(2026, 8, 15), "Breakfast", "Oats, whey, banana", 620, 40, 70, logged = true),
            Meal("m2", LocalDate.of(2026, 8, 16), "Dinner", kcal = 800),
            Meal("m3", LocalDate.of(2026, 9, 1), "Lunch"),
        )
        val file = mealsCsv(AUGUST, meals)

        assertEquals(2, file.rows)
        assertEquals(
            listOf(
                "Date,Meal,Detail,Kcal,Protein g,Carbs g,Logged",
                "2026-08-15,Breakfast,\"Oats, whey, banana\",620,40,70,yes",
                "2026-08-16,Dinner,,800,0,0,no",
            ),
            file.text.trimEnd().split("\r\n"),
        )
    }

    @Test
    fun `a sim ended early leaves its unraced legs blank`() {
        val early = RaceResult(at(LocalDate.of(2026, 8, 20)), seconds = 600, legs = listOf(270, 265))
        val lines = racesCsv(AUGUST, listOf(early), UTC).text.trimEnd().split("\r\n")

        assertEquals(3 + LEGS.size, lines[0].split(",").size)
        assertTrue(lines[1].startsWith("2026-08-20,0:10:00,no,4:30,4:25,,"))
        assertEquals(3 + LEGS.size, lines[1].split(",").size)
    }

    @Test
    fun `a month holding nothing is empty`() {
        assertTrue(exportMonth(AUGUST, emptyList(), emptyList(), emptyList(), UTC).isEmpty)
        assertFalse(exportMonth(AUGUST, listOf(squat), emptyList(), emptyList(), UTC).isEmpty)
    }

    @Test
    fun `cells carrying structure are quoted, and formulas are held off`() {
        assertEquals("plain", csvCell("plain"))
        assertEquals("\"a, b\"", csvCell("a, b"))
        assertEquals("\"say \"\"hi\"\"\"", csvCell("say \"hi\""))
        assertEquals("'=SUM(A1)", csvCell("=SUM(A1)"))
    }
}
