package com.repsrox.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {

    @Test
    fun `a full name is read as its first and last initials`() {
        assertEquals("ML", initialsOf("Ming Lim"))
        assertEquals("ML", initialsOf("Ming Yuen Lim"))
    }

    @Test
    fun `one word gives up one letter`() {
        assertEquals("M", initialsOf("Ming"))
    }

    @Test
    fun `initials are printed uppercase however the name was typed`() {
        assertEquals("ML", initialsOf("ming lim"))
    }

    @Test
    fun `spacing around and between words is not a word`() {
        assertEquals("ML", initialsOf("  Ming   Lim  "))
    }

    @Test
    fun `letters are read past the punctuation a word opens with`() {
        assertEquals("ML", initialsOf("(Ming) 'Lim'"))
    }

    @Test
    fun `a name with nothing letterable in it falls back to the placeholder`() {
        assertEquals("—", initialsOf(""))
        assertEquals("—", initialsOf("   "))
        assertEquals("—", initialsOf("-- ..."))
    }
}
