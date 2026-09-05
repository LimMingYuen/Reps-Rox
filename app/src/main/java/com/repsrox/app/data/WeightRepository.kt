package com.repsrox.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.weightStore: DataStore<Preferences> by preferencesDataStore(name = "weight")

private val LOG_KEY = stringPreferencesKey("weigh_ins")

/**
 * The weigh-in log, on disk. A weight tracker gains a row a day at the very
 * most, so the whole log lives in one preference as newline-separated records
 * rather than in a database that would earn nothing here.
 *
 * A record is `date|kg`: `2026-08-15|81.4`.
 *
 * Nothing is seeded. An install that has weighed in nowhere reads as an empty
 * log, which the Body screen shows as its empty state rather than as a trend
 * drawn through numbers nobody stood on a scale for.
 */
class WeightRepository(context: Context) {

    private val store = context.applicationContext.weightStore

    /** Oldest first — the order both the chart and the trend want. */
    val weighIns: Flow<List<WeighIn>> = store.data.map { it.log() }

    /** Records a weigh-in, replacing whatever was already logged for that date. */
    suspend fun add(entry: WeighIn) = update { log -> mergeInto(log, entry) }

    suspend fun remove(date: LocalDate) = update { log ->
        log.filterNot { it.date == date }
    }

    private suspend fun update(transform: (List<WeighIn>) -> List<WeighIn>) {
        store.edit { prefs ->
            prefs[LOG_KEY] = encodeLog(transform(prefs.log()).sortedBy { it.date })
        }
    }

    /** Nothing has ever been written is an empty log, not a seeded one. */
    private fun Preferences.log(): List<WeighIn> = this[LOG_KEY]?.let(::decodeLog).orEmpty()
}

/** Puts [entry] into [log], replacing the weigh-in already held for that date. */
internal fun mergeInto(log: List<WeighIn>, entry: WeighIn): List<WeighIn> =
    log.filterNot { it.date == entry.date } + entry

internal fun encodeLog(log: List<WeighIn>): String = log.joinToString("\n") { entry ->
    "${entry.date}|${entry.kg}"
}

/**
 * Skips anything it cannot read rather than losing the whole log to one bad line.
 * Records carrying a third field are logs written before the waist was dropped;
 * they still read as weigh-ins, so no migration is needed.
 */
internal fun decodeLog(raw: String): List<WeighIn> = raw.lineSequence()
    .mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size < 2) return@mapNotNull null
        val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return@mapNotNull null
        val kg = parts[1].toFloatOrNull() ?: return@mapNotNull null
        WeighIn(date, kg)
    }
    .sortedBy { it.date }
    .toList()
