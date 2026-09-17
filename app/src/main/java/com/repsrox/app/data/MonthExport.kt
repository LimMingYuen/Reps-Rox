package com.repsrox.app.data

import java.time.YearMonth
import java.time.ZoneId

/**
 * A month of what was actually done, as CSV — one file each for training, meals
 * and races, so each opens as its own sheet. Unlike [exportPlan] nothing here is
 * read back in: this is the record going out, not a plan coming round again.
 */
data class MonthExport(val month: YearMonth, val files: List<CsvFile>) {
    val isEmpty: Boolean get() = files.all { it.rows == 0 }
}

/** One sheet. [rows] leaves the header out, so an empty month counts as zero. */
data class CsvFile(val label: String, val name: String, val rows: Int, val text: String)

fun exportMonth(
    month: YearMonth,
    sessions: List<Session>,
    meals: List<Meal>,
    races: List<RaceResult>,
    zone: ZoneId = ZoneId.systemDefault(),
): MonthExport = MonthExport(
    month,
    listOf(
        trainingCsv(month, sessions, zone),
        mealsCsv(month, meals),
        racesCsv(month, races, zone),
    ),
)

/** A row a set, so a sheet can total volume or chart a lift without unpicking a cell. */
internal fun trainingCsv(month: YearMonth, sessions: List<Session>, zone: ZoneId): CsvFile {
    val rows = sessions
        .filter { YearMonth.from(it.day(zone)) == month }
        .sortedBy { it.finishedAt }
        .flatMap { session ->
            session.exercises.flatMap { exercise ->
                exercise.sets.mapIndexed { index, set ->
                    listOf(
                        session.day(zone).toString(),
                        session.name,
                        formatMinutes(session.seconds),
                        exercise.name,
                        (index + 1).toString(),
                        set.reps.toString(),
                        if (set.unit == SetUnit.METRES) "m" else "reps",
                        set.kg,
                    )
                }
            }
        }
    return csvFile(
        "Training",
        "training",
        month,
        listOf("Date", "Session", "Duration", "Exercise", "Set", "Amount", "Unit", "Weight kg"),
        rows,
    )
}

/** Every meal the month held, eaten or not — the Logged column says which. */
internal fun mealsCsv(month: YearMonth, meals: List<Meal>): CsvFile {
    val rows = meals
        .filter { YearMonth.from(it.date) == month }
        .sortedBy { it.date }
        .map { meal ->
            listOf(
                meal.date.toString(),
                meal.name,
                meal.detail,
                meal.kcal.toString(),
                meal.proteinG.toString(),
                meal.carbsG.toString(),
                if (meal.logged) "yes" else "no",
            )
        }
    return csvFile(
        "Meals",
        "meals",
        month,
        listOf("Date", "Meal", "Detail", "Kcal", "Protein g", "Carbs g", "Logged"),
        rows,
    )
}

/** A row a sim, with a column a leg. A sim ended early leaves its unraced legs blank. */
internal fun racesCsv(month: YearMonth, races: List<RaceResult>, zone: ZoneId): CsvFile {
    val rows = races
        .filter { YearMonth.from(it.finishedAt.atZone(zone).toLocalDate()) == month }
        .sortedBy { it.finishedAt }
        .map { race ->
            listOf(
                race.finishedAt.atZone(zone).toLocalDate().toString(),
                formatHours(race.seconds),
                if (race.complete) "yes" else "no",
            ) + LEGS.indices.map { index -> race.legs.getOrNull(index)?.let(::formatMinutes).orEmpty() }
        }
    return csvFile(
        "Races",
        "races",
        month,
        listOf("Date", "Time", "Complete") + LEGS.map { "${it.tag} ${it.name}" },
        rows,
    )
}

private fun csvFile(
    label: String,
    slug: String,
    month: YearMonth,
    header: List<String>,
    rows: List<List<String>>,
) = CsvFile(
    label = label,
    name = "repsrox-$slug-$month.csv",
    rows = rows.size,
    // CRLF is what the CSV spec asks for, and what a spreadsheet expects.
    text = (listOf(header) + rows).joinToString("\r\n", postfix = "\r\n") { it.joinToString(",", transform = ::csvCell) },
)

/**
 * Quotes a cell that carries structure. One opening with a formula character is
 * held off with an apostrophe, so a meal named "=SUM(…" opens as text.
 */
internal fun csvCell(value: String): String {
    val safe = if (value.isNotEmpty() && value[0] in FORMULA_LEADS) "'$value" else value
    return if (safe.any { it in "\",\r\n" }) "\"${safe.replace("\"", "\"\"")}\"" else safe
}

private val FORMULA_LEADS = setOf('=', '+', '-', '@')
