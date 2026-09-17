package com.repsrox.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RaceLogTest {

    private fun result(epoch: Long, seconds: Int, legs: Int) =
        RaceResult(Instant.ofEpochSecond(epoch), seconds, List(legs) { 200 + it })

    @Test
    fun `a log survives the round trip, newest first`() {
        val older = result(1_000, 5_400, LEGS.size)
        val newer = result(2_000, 1_300, 5)
        assertEquals(listOf(newer, older), decodeRaceLog(encodeRaceLog(listOf(older, newer))))
    }

    @Test
    fun `a sim ended before its first leg closed still reads`() {
        val result = result(1_000, 95, 0)
        assertEquals(result, decodeRaceResult(encodeRaceResult(result)))
    }

    @Test
    fun `a record that cannot be read is skipped without taking the log with it`() {
        val good = result(1_000, 5_400, 3)
        val raw = "nonsense\n${encodeRaceResult(good)}\n1|2|x,y\n5|0|\n"
        assertEquals(listOf(good), decodeRaceLog(raw))
    }

    @Test
    fun `more legs than the course has is not this course`() {
        assertNull(decodeRaceResult(encodeRaceResult(result(1_000, 9_000, LEGS.size + 1))))
    }

    @Test
    fun `only a sim closed to its last leg is complete, and only those contend for best`() {
        val early = result(3_000, 900, 4)
        val slow = result(1_000, 6_000, LEGS.size)
        val quick = result(2_000, 5_400, LEGS.size)
        assertFalse(early.complete)
        assertTrue(quick.complete)
        assertEquals(quick, bestRace(listOf(early, slow, quick)))
        assertNull(bestRace(listOf(early)))
    }

    @Test
    fun `stations closed counts stations, not legs`() {
        val legs = 5
        assertEquals(LEGS.take(legs).count { it.isStation }, result(1_000, 1_300, legs).stationsClosed)
    }
}
