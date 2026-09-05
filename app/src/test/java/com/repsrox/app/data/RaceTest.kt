package com.repsrox.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

private val TODAY: LocalDate = LocalDate.of(2026, 8, 15)

class RaceCountdownTest {

    private fun countdown(date: LocalDate) = countdownTo(date, TODAY)

    @Test
    fun `the days either side of today are named rather than counted`() {
        assertEquals("today", countdown(TODAY))
        assertEquals("tomorrow", countdown(TODAY.plusDays(1)))
        assertEquals("yesterday", countdown(TODAY.minusDays(1)))
    }

    @Test
    fun `days count up to three weeks out, and weeks take over from there`() {
        assertEquals("in 6 days", countdown(TODAY.plusDays(6)))
        assertEquals("in 20 days", countdown(TODAY.plusDays(20)))
        assertEquals("in 3 weeks", countdown(TODAY.plusDays(21)))
        assertEquals("in 11 weeks", countdown(TODAY.plusWeeks(11)))
    }

    @Test
    fun `a race already run counts backwards in the same phrasing`() {
        assertEquals("4 days ago", countdown(TODAY.minusDays(4)))
        assertEquals("5 weeks ago", countdown(TODAY.minusWeeks(5)))
    }

    @Test
    fun `a date carries its year only when it is not this one`() {
        assertEquals("14 Nov", raceDateLabel(LocalDate.of(2026, 11, 14), TODAY))
        assertEquals("14 Nov 2027", raceDateLabel(LocalDate.of(2027, 11, 14), TODAY))
    }
}

class RaceCodecTest {

    @Test
    fun `a booked race survives the round trip`() {
        val race = Race("HYROX London", LocalDate.of(2026, 11, 14))

        assertEquals(race, decodeRace(encodeRace(race)))
    }

    @Test
    fun `a name carrying the separator is stripped rather than allowed to break the record`() {
        val encoded = encodeRace(Race("HYROX | London", LocalDate.of(2026, 11, 14)))

        assertEquals("HYROX  London", decodeRace(encoded)?.name)
    }

    @Test
    fun `a record without a readable date reads as nothing booked`() {
        assertNull(decodeRace(""))
        assertNull(decodeRace("someday|HYROX London"))
    }

    @Test
    fun `a record that lost its name still reads as a race`() {
        assertEquals(Race("Race", LocalDate.of(2026, 11, 14)), decodeRace("2026-11-14|"))
    }
}
