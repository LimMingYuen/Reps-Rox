package com.repsrox.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.rollingStore: DataStore<Preferences> by preferencesDataStore(name = "rolling-plan")

private val ROLLING_KEY = stringPreferencesKey("week")

/**
 * The one week the app repeats — the plan every week ahead is printed from until
 * it is replaced. Written with the saved plans' own codec, since a rolling plan
 * is the same thing as a saved one; what differs is that this one is in force.
 */
class RollingPlanRepository(context: Context) {

    private val store = context.applicationContext.rollingStore

    /** Null while nothing is in force — the app then shows only what is written down. */
    val plan: Flow<WeekTemplate?> = store.data.map { prefs ->
        prefs[ROLLING_KEY]?.let { decodeTemplates(it).firstOrNull() }
    }

    /** Puts [template] in force, or clears the plan when it is null. */
    suspend fun set(template: WeekTemplate?) {
        store.edit { prefs ->
            if (template == null) prefs.remove(ROLLING_KEY) else prefs[ROLLING_KEY] = encodeTemplates(listOf(template))
        }
    }
}
