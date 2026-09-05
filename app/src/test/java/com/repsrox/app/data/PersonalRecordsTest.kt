package com.repsrox.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

private val ZONE: ZoneId = ZoneId.of("UTC")
private val TODAY: LocalDate = LocalDate.of(2026, 8, 15)

private fun session(day: LocalDate, vararg exercises: LoggedExercise) = Session(
    finishedAt = day.atStartOfDay(ZONE).toInstant(),
    name = "Lower push",
    seconds = 3_600,
    exercises = exercises.toList(),
)

private fun lift(name: String, vararg sets: WorkSet) = LoggedExercise(name, sets.toList())

private fun reps(count: Int, kg: String) = WorkSet(count, kg, SetUnit.REPS)

private fun metres(distance: Int, kg: String) = WorkSet(distance, kg, SetUnit.METRES)

class BestLiftsTest {

    private fun bests(log: List<Session>) = bestLifts(log, TODAY, ZONE)

    @Test
    fun `a lift is remembered by its heaviest set`() {
        val log = listOf(
            session(TODAY.minusMonths(2), lift("Back squat", reps(5, "120"), reps(5, "140"))),
            session(TODAY.minusDays(3), lift("Back squat", reps(8, "110"))),
        )

        assertEquals(listOf(PersonalRecord("Back squat", "140 kg × 5", "Jun")), bests(log))
    }

    @Test
    fun `metres carry a sled load rather than a lift, so they hold no record`() {
        val log = listOf(
            session(
                TODAY,
                lift("Sled push", metres(25, "150")),
                lift("Back squat", reps(5, "120")),
            ),
        )

        assertEquals(listOf(PersonalRecord("Back squat", "120 kg × 5", "Aug")), bests(log))
    }

    @Test
    fun `bodyweight work holds no record either`() {
        val log = listOf(session(TODAY, lift("Pull-up", reps(12, "0"), reps(10, ""))))

        assertTrue(bests(log).isEmpty())
    }

    @Test
    fun `reps break a tie at the same weight`() {
        val log = listOf(
            session(TODAY.minusDays(7), lift("Deadlift", reps(1, "180"))),
            session(TODAY, lift("Deadlift", reps(3, "180"))),
        )

        assertEquals(listOf(PersonalRecord("Deadlift", "180 kg × 3", "Aug")), bests(log))
    }

    @Test
    fun `a record is dated the day it was first hit, not the last time it stood`() {
        val first = TODAY.minusMonths(3)
        val log = listOf(
            session(TODAY, lift("Bench press", reps(5, "100"))),
            session(first, lift("Bench press", reps(5, "100"))),
        )

        assertEquals("May", bests(log).single().whenLabel)
    }

    @Test
    fun `a record set in an earlier year carries the year with it`() {
        val log = listOf(session(TODAY.minusYears(1), lift("Bench press", reps(5, "100"))))

        assertEquals("Aug 25", bests(log).single().whenLabel)
    }

    @Test
    fun `the board reads heaviest first`() {
        val log = listOf(
            session(
                TODAY,
                lift("Bench press", reps(5, "100")),
                lift("Deadlift", reps(5, "180")),
                lift("Back squat", reps(5, "140")),
            ),
        )

        assertEquals(listOf("Deadlift", "Back squat", "Bench press"), bests(log).map { it.name })
    }

    @Test
    fun `a lift typed two ways is still one lift`() {
        val log = listOf(
            session(TODAY.minusDays(9), lift("Back squat", reps(5, "140"))),
            session(TODAY, lift("back squat", reps(5, "150"))),
        )

        assertEquals(listOf(PersonalRecord("back squat", "150 kg × 5", "Aug")), bests(log))
    }

    @Test
    fun `an empty log holds no records at all`() {
        assertTrue(bests(emptyList()).isEmpty())
    }

    @Test
    fun `a half-kilo load keeps its decimal, a whole one drops it`() {
        val log = listOf(
            session(TODAY, lift("Bench press", reps(5, "102.5")), lift("Deadlift", reps(5, "180"))),
        )

        assertEquals(listOf("180 kg × 5", "102.5 kg × 5"), bests(log).map { it.value })
    }
}
