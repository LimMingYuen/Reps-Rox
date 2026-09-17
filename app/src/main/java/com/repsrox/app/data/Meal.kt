package com.repsrox.app.data

import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

/**
 * One thing eaten, or planned to be. A meal hangs off a date rather than off a
 * session, so the fuel screen reads a day at a time and the plan document can
 * write meals out under the same dates its sessions use.
 */
data class Meal(
    val id: String,
    val date: LocalDate,
    val name: String,
    /** What is actually on the plate — "Oats, whey, banana". */
    val detail: String = "",
    val kcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    /** Whether it has been eaten. Planning a meal never checks it off. */
    val logged: Boolean = false,
) {
    /** The second line of a meal row: what's in it, or what it is worth. */
    fun meta(): String = detail.ifBlank { macroLine().ifBlank { "Not logged" } }

    /** "40 g protein · 70 g carbs", dropping whichever figure was left at zero. */
    fun macroLine(): String = listOfNotNull(
        proteinG.takeIf { it > 0 }?.let { "$it g protein" },
        carbsG.takeIf { it > 0 }?.let { "$it g carbs" },
    ).joinToString(" · ")

    /** The kcal column: a dash rather than a zero, since nothing is not none. */
    fun kcalLabel(): String = if (kcal > 0) formatKcal(kcal) else "—"
}

/** The ranges a meal is held to; outside them the entry is a slip, not a meal. */
val KCAL_RANGE = 0..5_000
val MACRO_RANGE_G = 0..500
const val MEAL_NAME_MAX_CHARS = 40
const val MEAL_DETAIL_MAX_CHARS = 60

/**
 * The day's targets. Calories are the only one training moves — the design's
 * 2,540 is a training day's figure, which is the base plus the bonus.
 */
const val BASE_TARGET_KCAL = 2_220
const val TRAINING_KCAL = 320
const val TARGET_PROTEIN_G = 165
const val TARGET_CARBS_G = 290

/** What a day is aiming at: the base, plus the bonus a day that trains earns. */
fun targetKcal(training: Boolean): Int = BASE_TARGET_KCAL + if (training) TRAINING_KCAL else 0

/** One macro's row on the fuel screen: what's banked against what's aimed at. */
data class Macro(val label: String, val value: String, val percent: Int, val accented: Boolean)

/**
 * What a day's eating adds up to. Only logged meals count towards the macros —
 * a meal still on the plan is intent, not intake — while [plannedKcal] carries
 * the whole day so you can see what is still to come.
 */
data class FuelSummary(
    val targetKcal: Int,
    val loggedKcal: Int,
    val loggedProteinG: Int,
    val loggedCarbsG: Int,
    val plannedKcal: Int,
    val logged: Int,
    val meals: Int,
) {
    val macros: List<Macro> get() = listOf(
        macro("Calories", loggedKcal, targetKcal, "", accented = false),
        macro("Protein", loggedProteinG, TARGET_PROTEIN_G, " g", accented = true),
        macro("Carbs", loggedCarbsG, TARGET_CARBS_G, " g", accented = false),
    )

    /** "2 of 4 checked in · 1,400 kcal still planned", or what's left of that. */
    fun meta(): String {
        if (meals == 0) return "Nothing planned"
        val remaining = plannedKcal - loggedKcal
        return listOfNotNull(
            "$logged of $meals checked in",
            remaining.takeIf { it > 0 }?.let { "${formatKcal(it)} kcal still planned" },
        ).joinToString(" · ")
    }
}

/** Reduces a single day's meals to the figures the fuel screen shows. */
fun summariseFuel(meals: List<Meal>, targetKcal: Int = targetKcal(training = false)): FuelSummary {
    val logged = meals.filter { it.logged }
    return FuelSummary(
        targetKcal = targetKcal,
        loggedKcal = logged.sumOf { it.kcal },
        loggedProteinG = logged.sumOf { it.proteinG },
        loggedCarbsG = logged.sumOf { it.carbsG },
        plannedKcal = meals.sumOf { it.kcal },
        logged = logged.size,
        meals = meals.size,
    )
}

private fun macro(label: String, banked: Int, target: Int, unit: String, accented: Boolean) = Macro(
    label = label,
    value = "${formatKcal(banked)} / ${formatKcal(target)}$unit",
    percent = if (target <= 0) 0 else (banked * 100f / target).roundToInt(),
    accented = accented,
)

/** Thousands are grouped, the way a calorie figure is read: "2,540". */
fun formatKcal(value: Int): String = String.format(Locale.US, "%,d", value)

// ── The rolling meal week ───────────────────────────────────────────────────

/**
 * Eating repeats the way training does: an imported document's meal days are read
 * as days of the week and printed onto every day from today on that holds nothing
 * of its own. Printed days are not written to disk, so a new document changes every
 * day ahead at once.
 *
 * Meals roll a day at a time rather than a week at a time — checking breakfast in
 * on Monday should not freeze what Thursday is going to be. The first change to a
 * printed day writes that day down (see [MealRepository]), and it is its own
 * business from then on, even if every meal on it is later deleted.
 */
const val ROLLING_MEAL_PREFIX = "rolling-meal"

/** The id a meal printed from the rolling week carries — the day it fell on, and where in it. */
fun rollingMealId(date: LocalDate, index: Int): String = "$ROLLING_MEAL_PREFIX-$date-$index"

/** The day a printed meal belongs to, or null for a meal that was entered by hand. */
fun rollingMealDay(id: String): LocalDate? {
    if (!id.startsWith("$ROLLING_MEAL_PREFIX-")) return null
    val date = id.removePrefix("$ROLLING_MEAL_PREFIX-").substringBeforeLast("-")
    return runCatching { LocalDate.parse(date) }.getOrNull()
}

/**
 * A document's meal days as a week worth repeating. The meals keep a date only to
 * say which day of the week they fall on. A document longer than a week names the
 * same weekday twice; the earlier day wins, as it does for sessions.
 */
fun mealWeek(days: Map<LocalDate, List<Meal>>): List<Meal> =
    days.toSortedMap().entries
        .distinctBy { it.key.dayOfWeek }
        .flatMap { (date, meals) -> meals.map { it.copy(date = date, logged = false) } }

/** What [rolling] puts on [date]: that weekday's meals, none of them eaten yet. */
fun printMealDay(rolling: List<Meal>, date: LocalDate): List<Meal> =
    rolling.filter { it.date.dayOfWeek == date.dayOfWeek }
        .mapIndexed { index, meal -> meal.copy(id = rollingMealId(date, index), date = date, logged = false) }

/**
 * The meals as the app reads them: what is stored, plus [rolling] printed onto
 * every day from [today] on that holds nothing of its own and was never
 * [written] down. Days behind you are left as they were — a plan says what you
 * will eat, and cannot say what you ate.
 */
fun projectMeals(
    stored: List<Meal>,
    rolling: List<Meal>,
    written: Set<LocalDate>,
    today: LocalDate,
    weeks: Int = ROLLING_HORIZON_WEEKS,
): List<Meal> {
    if (rolling.isEmpty()) return stored
    val spokenFor = stored.mapTo(written.toMutableSet()) { it.date }
    val printed = generateSequence(today) { it.plusDays(1) }
        .takeWhile { it.isBefore(today.weekStart().plusWeeks(weeks.toLong())) }
        .filterNot { it in spokenFor }
        .flatMap { printMealDay(rolling, it) }
    return (stored + printed).sortedBy { it.date }
}

/**
 * [date] written down as the rolling week currently prints it, or null when there
 * is nothing to write: the day is behind you, already holds meals of its own, was
 * written down before, or the week has nothing for it.
 */
fun writeDownMealDay(
    stored: List<Meal>,
    rolling: List<Meal>,
    written: Set<LocalDate>,
    date: LocalDate,
    today: LocalDate,
): List<Meal>? {
    if (date.isBefore(today) || date in written || stored.any { it.date == date }) return null
    val printed = printMealDay(rolling, date).ifEmpty { return null }
    return (stored + printed).sortedBy { it.date }
}

/**
 * The days an imported document lands on: the dates it names, plus its weekday's
 * meals on every day from [today] on that was already written down — those days
 * no longer print from the plan, and a new plan that changed every day but the
 * ones you had touched would read as not having landed. A day the document names
 * outright is never overwritten by a weekday's copy.
 */
fun importMealDays(
    days: Map<LocalDate, List<Meal>>,
    stored: List<Meal>,
    written: Set<LocalDate>,
    today: LocalDate,
): Map<LocalDate, List<Meal>> {
    val rolling = mealWeek(days)
    val carried = stored.mapTo(written.toMutableSet()) { it.date }
        .filter { !it.isBefore(today) && it !in days.keys }
        .mapNotNull { date -> printMealDay(rolling, date).takeIf { it.isNotEmpty() }?.let { date to it } }
    return days + carried
}

/**
 * Lays imported days over [current]. A date the document carries replaces that
 * date's meals outright; every other date is left alone, so importing one day
 * never disturbs the rest of the week.
 *
 * Check-ins survive a re-import: a meal coming back under a name already
 * checked off that day comes back checked off, since a document describes what
 * you plan to eat and says nothing about what you have eaten.
 */
fun applyMealDays(current: List<Meal>, days: Map<LocalDate, List<Meal>>): List<Meal> {
    if (days.isEmpty()) return current
    val loggedNames = current.filter { it.logged }
        .groupBy({ it.date }, { it.name.lowercase() })
        .mapValues { (_, names) -> names.toSet() }

    val replaced = days.entries.flatMap { (date, meals) ->
        val alreadyLogged = loggedNames[date].orEmpty()
        meals.map { it.copy(date = date, logged = it.name.lowercase() in alreadyLogged) }
    }
    return (current.filterNot { it.date in days.keys } + replaced).sortedBy { it.date }
}
