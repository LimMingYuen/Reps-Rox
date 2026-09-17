package com.repsrox.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

private val Context.raceLogStore: DataStore<Preferences> by preferencesDataStore(name = "race_log")

private val LOG_KEY = stringPreferencesKey("races")

/**
 * Every sim raced, on disk. Same reasoning as [SessionRepository]: a handful of
 * records read whole every time, so one preference holding newline-separated
 * records beats a table. Kept apart from [RaceRepository], which holds the race
 * being trained for rather than the ones already run.
 */
class RaceLogRepository(context: Context) {

    private val store = context.applicationContext.raceLogStore

    /** Newest first. */
    val races: Flow<List<RaceResult>> = store.data.map { prefs ->
        prefs[LOG_KEY]?.let(::decodeRaceLog).orEmpty()
    }

    suspend fun bank(result: RaceResult) {
        store.edit { prefs ->
            val log = prefs[LOG_KEY]?.let(::decodeRaceLog).orEmpty()
            prefs[LOG_KEY] = encodeRaceLog((log + result).sortedByDescending { it.finishedAt })
        }
    }

    suspend fun remove(finishedAt: Instant) {
        store.edit { prefs ->
            val log = prefs[LOG_KEY]?.let(::decodeRaceLog).orEmpty()
            prefs[LOG_KEY] = encodeRaceLog(log.filterNot { it.finishedAt == finishedAt })
        }
    }
}
