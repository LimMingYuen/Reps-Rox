package com.repsrox.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileStore: DataStore<Preferences> by preferencesDataStore(name = "profile")

private val NAME_KEY = stringPreferencesKey("name")
private val REST_KEY = intPreferencesKey("rest_seconds")

/** How long the rest clock runs after a set, until the athlete says otherwise. */
const val DEFAULT_REST_SECONDS = 105
const val REST_STEP_SECONDS = 15
val REST_RANGE_SECONDS = 15..600

/**
 * Who the training belongs to, on disk. One field so far, so this holds a bare
 * string rather than a record of its own.
 *
 * Nothing is seeded, for the same reason [RaceRepository] seeds nothing: an
 * install nobody has named reads as unnamed, which is a true answer rather than
 * a name the profile would have to pretend was the owner's.
 */
class ProfileRepository(context: Context) {

    private val store = context.applicationContext.profileStore

    /** Null while no name has been set — including on a fresh install. */
    val name: Flow<String?> = store.data.map { prefs -> prefs[NAME_KEY]?.trimmed() }

    /** The rest between sets, as last set on the live tracker. */
    val restSeconds: Flow<Int> = store.data.map { prefs ->
        (prefs[REST_KEY] ?: DEFAULT_REST_SECONDS).coerceIn(REST_RANGE_SECONDS)
    }

    suspend fun setRestSeconds(seconds: Int) {
        store.edit { prefs -> prefs[REST_KEY] = seconds.coerceIn(REST_RANGE_SECONDS) }
    }

    /** Sets the name, or clears it when what was typed is only whitespace. */
    suspend fun setName(name: String) {
        val trimmed = name.trimmed()
        store.edit { prefs ->
            if (trimmed == null) prefs.remove(NAME_KEY) else prefs[NAME_KEY] = trimmed
        }
    }
}

/** Newlines are not part of a name; a name left blank is no name at all. */
private fun String.trimmed(): String? =
    filterNot { it == '\n' }.trim().takeIf { it.isNotEmpty() }
