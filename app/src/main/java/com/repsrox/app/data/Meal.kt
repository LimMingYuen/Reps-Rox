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
