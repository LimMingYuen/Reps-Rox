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

private val Context.mealStore: DataStore<Preferences> by preferencesDataStore(name = "meals")

private val MEALS_KEY = stringPreferencesKey("meals")

/**
 * The meal log, on disk. A day gains a handful of rows at most, so the whole log
 * lives in one preference as newline-separated records — the same bargain
 * [WeightRepository] and [PlanRepository] strike, for the same reason.
 *
 * A record is `id|date|name|detail|kcal|protein|carbs|logged`:
 *
 *     m1|2026-08-15|Breakfast|Oats, whey, banana|620|40|70|1
 *
 * Nothing is seeded. A day nobody has planned reads as an empty list, which the
 * Fuel screen shows as its empty state rather than as meals the athlete never
 * chose and would have to delete before the day meant anything.
 */
class MealRepository(context: Context) {

    private val store = context.applicationContext.mealStore

    /** Earliest first — the order a week of meals reads in. */
    val meals: Flow<List<Meal>> = store.data.map { it.log() }

    /** Writes a meal, replacing whatever was held under the same id. */
    suspend fun save(meal: Meal) = update { log ->
        log.filterNot { it.id == meal.id } + meal
    }

    suspend fun remove(id: String) = update { log ->
        log.filterNot { it.id == id }
    }

    suspend fun setLogged(id: String, logged: Boolean) = update { log ->
        log.map { if (it.id == id) it.copy(logged = logged) else it }
    }

    /** Applies an imported document's meal days, a whole day at a time. */
    suspend fun applyDays(days: Map<LocalDate, List<Meal>>) = update { log ->
        applyMealDays(log, days)
    }

    private suspend fun update(transform: (List<Meal>) -> List<Meal>) {
        store.edit { prefs ->
            prefs[MEALS_KEY] = encodeMeals(transform(prefs.log()).sortedBy { it.date })
        }
    }

    /** Nothing has ever been written is an empty log, not a seeded one. */
    private fun Preferences.log(): List<Meal> = this[MEALS_KEY]?.let(::decodeMeals).orEmpty()
}

// ── Record format ───────────────────────────────────────────────────────────

internal fun encodeMeals(log: List<Meal>): String = log.joinToString("\n") { meal ->
    listOf(
        meal.id,
        meal.date.toString(),
        meal.name,
        meal.detail,
        meal.kcal.toString(),
        meal.proteinG.toString(),
        meal.carbsG.toString(),
        if (meal.logged) "1" else "0",
    ).joinToString(FIELD.toString())
}

/**
 * Skips anything it cannot read rather than losing the whole log to one bad
 * line — a meal missing its id, date or name is not a meal.
 */
internal fun decodeMeals(raw: String): List<Meal> = raw.lineSequence()
    .mapNotNull(::decodeMeal)
    .sortedBy { it.date }
    .toList()

private fun decodeMeal(line: String): Meal? {
    val fields = line.split(FIELD)
    if (fields.size < 8) return null
    val id = fields[0].takeIf { it.isNotBlank() } ?: return null
    val date = runCatching { LocalDate.parse(fields[1]) }.getOrNull() ?: return null
    val name = fields[2].takeIf { it.isNotBlank() } ?: return null
    return Meal(
        id = id,
        date = date,
        name = name,
        detail = fields[3],
        kcal = fields[4].toIntOrNull() ?: 0,
        proteinG = fields[5].toIntOrNull() ?: 0,
        carbsG = fields[6].toIntOrNull() ?: 0,
        logged = fields[7] == "1",
    )
}
