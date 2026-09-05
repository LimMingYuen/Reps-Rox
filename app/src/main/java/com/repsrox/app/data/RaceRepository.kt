package com.repsrox.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.raceStore: DataStore<Preferences> by preferencesDataStore(name = "race")

private val RACE_KEY = stringPreferencesKey("race")

/**
 * The booked race, on disk. Only ever one, so this holds a single record rather
 * than the newline-separated logs [WeightRepository] and [MealRepository] keep.
 *
 * A record is `date|name`: `2026-11-14|HYROX London`.
 *
 * Nothing is seeded. An install that has booked no race reads as none booked,
 * which is a true answer rather than sample content the profile would have to
 * pretend was real.
 */
class RaceRepository(context: Context) {

    private val store = context.applicationContext.raceStore

    /** Null while nothing is booked — including on a fresh install. */
    val race: Flow<Race?> = store.data.map { prefs -> prefs[RACE_KEY]?.let(::decodeRace) }

    /** Books a race, replacing whatever was booked before it. */
    suspend fun book(race: Race) {
        store.edit { prefs -> prefs[RACE_KEY] = encodeRace(race) }
    }

    suspend fun clear() {
        store.edit { prefs -> prefs.remove(RACE_KEY) }
    }
}
