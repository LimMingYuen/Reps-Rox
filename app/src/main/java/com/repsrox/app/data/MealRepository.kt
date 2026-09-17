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
import java.time.YearMonth

private val Context.mealStore: DataStore<Preferences> by preferencesDataStore(name = "meals")

private val MEALS_KEY = stringPreferencesKey("meals")

/** The week every day ahead repeats, in the log's own format — see [mealWeek]. */
private val ROLLING_KEY = stringPreferencesKey("rolling")

/** Days ahead that were written down and so no longer print from the rolling week. */
private val WRITTEN_KEY = stringPreferencesKey("written")

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

    /**
     * Earliest first — the order a week of meals reads in. This is the log as the
     * app reads it, with the rolling week printed onto the days ahead.
     */
    val meals: Flow<List<Meal>> = store.data.map { prefs ->
        projectMeals(prefs.log(), prefs.rolling(), prefs.written(), LocalDate.now())
    }

    /** Only what is on disk — a record of what was eaten has no use for days still to come. */
    val stored: Flow<List<Meal>> = store.data.map { it.log() }

    /** Whether a meal week is in force, and so whether there is one to stop. */
    val repeating: Flow<Boolean> = store.data.map { it.rolling().isNotEmpty() }

    /** Writes a meal, replacing whatever was held under the same id. */
    suspend fun save(meal: Meal) = update({ meal.date }) { log ->
        log.filterNot { it.id == meal.id } + meal
    }

    suspend fun remove(id: String) = update({ log -> log.dayOf(id) }) { log ->
        log.filterNot { it.id == id }
    }

    suspend fun setLogged(id: String, logged: Boolean) = update({ log -> log.dayOf(id) }) { log ->
        log.map { if (it.id == id) it.copy(logged = logged) else it }
    }

    /**
     * Applies an imported document: its meal days become the week every day ahead
     * repeats, and the dates it names — along with the days ahead already written
     * down — are laid over the log a whole day at a time.
     */
    suspend fun applyImport(days: Map<LocalDate, List<Meal>>, today: LocalDate = LocalDate.now()) {
        if (days.isEmpty()) return
        store.edit { prefs ->
            val log = prefs.log()
            prefs[ROLLING_KEY] = encodeMeals(mealWeek(days))
            prefs[MEALS_KEY] = encodeMeals(applyMealDays(log, importMealDays(days, log, prefs.written(), today)))
        }
    }

    /** Drops [month] from the log once it has been exported. Only a month already over is ever asked for, so no day ahead is touched. */
    suspend fun clearMonth(month: YearMonth) {
        store.edit { prefs -> prefs[MEALS_KEY] = encodeMeals(mealsWithout(month, prefs.log())) }
    }

    /** Stops repeating the meal week, leaving only the days already written down. */
    suspend fun clearRolling() {
        store.edit { it.remove(ROLLING_KEY) }
    }

    /**
     * Changes the log. A day the rolling week is printing holds nothing of its own,
     * so there is no meal there to edit, check in or delete; the day [touched] is
     * written down first, and the week stops speaking for it from then on.
     */
    private suspend fun update(touched: (List<Meal>) -> LocalDate?, transform: (List<Meal>) -> List<Meal>) {
        store.edit { prefs ->
            val today = LocalDate.now()
            val log = prefs.log()
            val written = prefs.written()
            val day = touched(log)
            val real = day?.let { writeDownMealDay(log, prefs.rolling(), written, it, today) } ?: log
            if (day != null && !day.isBefore(today)) {
                // Days behind you are never printed, so there is no use remembering them.
                prefs[WRITTEN_KEY] = (written + day).filterNot { it.isBefore(today) }.sorted().joinToString(",")
            }
            prefs[MEALS_KEY] = encodeMeals(transform(real).sortedBy { it.date })
        }
    }

    /** The day the meal under [id] falls on, whether it is stored or still only printed. */
    private fun List<Meal>.dayOf(id: String): LocalDate? = firstOrNull { it.id == id }?.date ?: rollingMealDay(id)

    /** Nothing has ever been written is an empty log, not a seeded one. */
    private fun Preferences.log(): List<Meal> = this[MEALS_KEY]?.let(::decodeMeals).orEmpty()

    private fun Preferences.rolling(): List<Meal> = this[ROLLING_KEY]?.let(::decodeMeals).orEmpty()

    private fun Preferences.written(): Set<LocalDate> = this[WRITTEN_KEY].orEmpty().split(",")
        .mapNotNullTo(mutableSetOf()) { runCatching { LocalDate.parse(it) }.getOrNull() }
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
