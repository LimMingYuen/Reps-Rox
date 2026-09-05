package com.repsrox.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Closes [count] legs, each exactly on its target. */
private fun onTarget(count: Int) = LEGS.take(count).map { it.targetSeconds }

class RaceMathsTest {

    @Test
    fun `a leg under its target reads negative`() {
        assertEquals(-5, legDelta(0, LEGS[0].targetSeconds - 5))
        assertEquals(0, legDelta(0, LEGS[0].targetSeconds))
        assertEquals(12, legDelta(0, LEGS[0].targetSeconds + 12))
    }

    @Test
    fun `a delta prints signed, with a true minus`() {
        assertEquals("\u22120:05", formatDelta(-5))
        assertEquals("0:00", formatDelta(0))
        assertEquals("+1:03", formatDelta(63))
    }

    @Test
    fun `averages are null until a leg of that kind is closed`() {
        assertNull(stationAverage(emptyList()))
        assertNull(runAverage(emptyList()))
        // Leg one is a run, so a station average still has nothing to average.
        assertNull(stationAverage(onTarget(1)))
        assertEquals(LEGS[0].targetSeconds, runAverage(onTarget(1)))
    }

    @Test
    fun `each average reads only its own kind of leg`() {
        // The first four legs: run, station, run, station.
        val closed = listOf(300, 280, 320, 200)
        assertEquals(310, runAverage(closed))
        assertEquals(240, stationAverage(closed))
    }

    @Test
    fun `a sim yet to start projects the plan`() {
        assertEquals(PLANNED_FINISH, projectedFinish(raceSeconds = 0, closed = emptyList()))
    }

    @Test
    fun `racing on target projects the plan`() {
        val closed = onTarget(4)
        assertEquals(PLANNED_FINISH, projectedFinish(closed.sum(), closed))
    }

    @Test
    fun `racing at twice the target projects twice the plan`() {
        val closed = onTarget(4).map { it * 2 }
        assertEquals(PLANNED_FINISH * 2, projectedFinish(closed.sum(), closed))
    }

    @Test
    fun `racing under target projects a faster finish`() {
        val closed = onTarget(4).map { it / 2 }
        val projected = projectedFinish(closed.sum(), closed)
        assertTrue("$projected is not under the plan", projected < PLANNED_FINISH)
        // Halving every closed leg loses a second to rounding here and there,
        // so this lands near half the plan rather than exactly on it.
        assertTrue("$projected is not near half the plan", projected > PLANNED_FINISH / 2 - 30)
    }

    @Test
    fun `a projection never finishes before the clock already shows`() {
        val closed = onTarget(4)
        // A very long leg still in progress is time spent, whatever the closed legs say.
        val elapsed = PLANNED_FINISH * 2
        assertEquals(elapsed, projectedFinish(elapsed, closed))
        assertTrue(projectedFinish(elapsed, emptyList()) >= elapsed)
    }

    @Test
    fun `a leg prints its own target`() {
        assertEquals("1 km \u00b7 target 4:35", LEGS[0].target)
        assertEquals(275, LEGS[0].targetSeconds)
    }
}
