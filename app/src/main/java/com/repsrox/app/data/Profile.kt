package com.repsrox.app.data

const val PROFILE_NAME_MAX_CHARS = 40

/** What the avatar prints while no name has been set. */
private const val NO_INITIALS = "—"

/**
 * The initials the avatar carries: the first letter of the first word and of
 * the last, so "Ming Yuen Lim" reads ML rather than MYL — a two-letter disc is
 * the shape the design draws and a third letter crowds it.
 *
 * A single word gives up its first letter alone. Letters are taken past any
 * punctuation the word opens with, so "(Ming)" still reads M, and a name with
 * nothing letterable in it falls back to the placeholder rather than printing
 * half a glyph.
 */
fun initialsOf(name: String): String {
    val words = name.trim().split(' ', '\t', '\n')
        .mapNotNull { word -> word.firstOrNull(Char::isLetterOrDigit) }
    return when (words.size) {
        0 -> NO_INITIALS
        1 -> words.first().uppercase()
        else -> "${words.first()}${words.last()}".uppercase()
    }
}
