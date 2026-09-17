package com.repsrox.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * The week as a Markdown document — exported so it can be edited by hand or
 * shared, and read back to update what a week asks of you. Two sections are
 * read on the way back in: `## Sessions`, which sets a day's exercises, and
 * `## Meals`, which sets a day's eating. `## Context` is printed for reference
 * and always ignored on the way back in, however table-shaped it looks.
 */
private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")

/** How far back the Context section's training/weigh-in tables look. */
private const val CONTEXT_DAYS = 28L

private enum class Section { SESSIONS, MEALS, CONTEXT, NONE }

/**
 * What a document read back. A session date with no matching session on the
 * plan it is applied to is silently dropped rather than reported — there is
 * nothing there to attach an edit to. Meals are not held to that: a meal day
 * needs only a date, so a document can plan eating for a day that carries no
 * session at all.
 */
data class ParsedPlan(
    val sessions: Map<LocalDate, List<Exercise>>,
    val meals: Map<LocalDate, List<Meal>> = emptyMap(),
    val problems: List<String> = emptyList(),
    /** What each session day was called, read off its heading — a plan needs names. */
    val names: Map<LocalDate, String> = emptyMap(),
) {
    val isEmpty: Boolean get() = sessions.isEmpty() && meals.isEmpty()

    fun summary(): String {
        if (isEmpty) return "Nothing to apply"
        val mealCount = meals.values.sumOf { it.size }
        val counts = listOfNotNull(
            sessions.size.takeIf { it > 0 }?.let { "$it ${plural(it, "session")}" },
            mealCount.takeIf { it > 0 }?.let { "$it ${plural(it, "meal")}" },
        )
        val dates = (sessions.keys + meals.keys).sorted().joinToString(", ")
        return (counts + dates).joinToString(" · ")
    }
}

// ── Export ──────────────────────────────────────────────────────────────────

fun exportPlan(
    plan: List<PlannedSession>,
    weekStart: LocalDate,
    meals: List<Meal> = emptyList(),
    banked: List<Session> = emptyList(),
    weighIns: List<WeighIn> = emptyList(),
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String = buildString {
    appendLine("# RepsRox plan · week of $weekStart")
    appendLine()
    appendLine(
        "Edit the sessions and meals below and import this text (paste it back, or " +
            "open the file) to update your week. Everything under Context is for " +
            "reference only and is never read back in.",
    )
    appendLine()
    appendLine("## Sessions")

    val strength = plan.filter { it.kind == SessionKind.STRENGTH }
    if (strength.isEmpty()) {
        appendLine()
        appendLine("Nothing planned.")
    }
    strength.forEach { session -> appendSessionTable(session) }

    appendLine()
    appendLine("## Meals")

    val byDay = meals.groupBy { it.date }.toSortedMap()
    if (byDay.isEmpty()) {
        appendLine()
        appendLine("Nothing planned.")
    }
    byDay.forEach { (date, day) -> appendMealTable(date, day) }

    appendLine()
    appendLine("## Context")
    appendLine()
    appendLine("### Recent training")
    appendRecentTraining(banked, today, zone)

    appendLine()
    appendLine("### Body")
    appendRecentWeighIns(weighIns, today)
}

private fun StringBuilder.appendSessionTable(session: PlannedSession) {
    appendLine()
    appendLine("### ${session.date} · ${session.name.cell()}")
    appendLine()
    appendLine("| Exercise | Sets | Reps | Weight |")
    appendLine("| --- | --- | --- | --- |")
    session.exercises.forEach { exercise ->
        val first = exercise.sets.firstOrNull()
        val reps = first?.let { if (it.unit == SetUnit.METRES) "${it.reps} m" else "${it.reps}" }.orEmpty()
        val weight = first?.kg?.ifBlank { "—" } ?: "—"
        appendLine("| ${exercise.name.cell()} | ${exercise.sets.size} | $reps | $weight |")
    }
}

/**
 * A day's eating. Check-ins are deliberately not written out: the document
 * plans what you will eat, and whether you ate it is the day's business rather
 * than the plan's.
 */
private fun StringBuilder.appendMealTable(date: LocalDate, meals: List<Meal>) {
    appendLine()
    appendLine("### $date")
    appendLine()
    appendLine("| Meal | Detail | Kcal | Protein (g) | Carbs (g) |")
    appendLine("| --- | --- | --- | --- | --- |")
    meals.forEach { meal ->
        appendLine(
            "| ${meal.name.cell()} | ${meal.detail.cell().ifBlank { "—" }} | " +
                "${meal.kcal.figure()} | ${meal.proteinG.figure()} | ${meal.carbsG.figure()} |",
        )
    }
}

private fun StringBuilder.appendRecentTraining(banked: List<Session>, today: LocalDate, zone: ZoneId) {
    val recent = banked.filter { !it.day(zone).isBefore(today.minusDays(CONTEXT_DAYS)) }
        .sortedByDescending { it.finishedAt }
    if (recent.isEmpty()) {
        appendLine()
        appendLine("No sessions banked in the last $CONTEXT_DAYS days.")
        return
    }
    appendLine()
    appendLine("| Date | Session | Exercises |")
    appendLine("| --- | --- | --- |")
    recent.forEach { session ->
        val detail = session.exercises.joinToString("; ") { "${it.name} ${it.detail()}" }
        appendLine("| ${session.day(zone)} | ${session.name.cell()} | ${detail.cell()} |")
    }
}

private fun StringBuilder.appendRecentWeighIns(weighIns: List<WeighIn>, today: LocalDate) {
    val recent = weighIns.filter { !it.date.isBefore(today.minusDays(CONTEXT_DAYS)) }
        .sortedByDescending { it.date }
    if (recent.isEmpty()) {
        appendLine()
        appendLine("No weigh-ins in the last $CONTEXT_DAYS days.")
        return
    }
    appendLine()
    appendLine("| Date | Weight |")
    appendLine("| --- | --- |")
    recent.forEach { appendLine("| ${it.date} | ${formatKilos(it.kg)} kg |") }
}

/** The characters the table format claims, stripped so a cell can't break its row. */
private fun String.cell(): String = replace("|", "/").replace("\n", " ").trim()

/** A zero is nothing rather than none, so it writes as the dash it reads back as. */
private fun Int.figure(): String = if (this > 0) toString() else "—"

// ── Import ──────────────────────────────────────────────────────────────────

fun parsePlan(markdown: String): ParsedPlan {
    val sessions = mutableMapOf<LocalDate, List<Exercise>>()
    val names = mutableMapOf<LocalDate, String>()
    val meals = mutableMapOf<LocalDate, List<Meal>>()
    val problems = mutableListOf<String>()

    var section = Section.NONE
    var heading = ""
    var date: LocalDate? = null
    var header: List<String>? = null
    var exercises = mutableListOf<Exercise>()
    var day = mutableListOf<Meal>()

    fun flush() {
        val d = date
        if (d != null) {
            if (section == Section.SESSIONS && exercises.isNotEmpty()) {
                sessions[d] = exercises.toList()
                sessionName(heading)?.let { names[d] = it }
            }
            if (section == Section.MEALS && day.isNotEmpty()) meals[d] = day.toList()
        }
        exercises = mutableListOf()
        day = mutableListOf()
        date = null
        header = null
    }

    markdown.lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.startsWith("## ") -> {
                flush()
                val title = line.drop(3).trim().lowercase()
                section = when {
                    title.contains("session") -> Section.SESSIONS
                    title.contains("meal") -> Section.MEALS
                    else -> Section.CONTEXT
                }
            }
            line.startsWith("### ") && (section == Section.SESSIONS || section == Section.MEALS) -> {
                flush()
                heading = line.drop(4).trim()
                val found = ISO_DATE.find(heading)?.value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                if (found == null) problems += "No date in heading: $heading" else date = found
            }
            line.startsWith("### ") -> flush()
            date != null && line.startsWith("|") -> {
                val cells = splitRow(line)
                when {
                    isSeparatorRow(cells) -> Unit
                    header == null -> header = cells.map { it.lowercase() }
                    section == Section.SESSIONS -> {
                        val exercise = readExercise(header!!, cells)
                        if (exercise == null) {
                            problems += "Could not read a row under $heading"
                        } else {
                            exercises.add(exercise)
                        }
                    }
                    section == Section.MEALS -> {
                        val meal = readMeal(header!!, cells, id = "$date#${day.size}", date = date!!)
                        if (meal == null) {
                            problems += "Could not read a meal under $heading"
                        } else {
                            day.add(meal)
                        }
                    }
                    else -> Unit
                }
            }
            else -> Unit
        }
    }
    flush()

    return ParsedPlan(sessions, meals, problems, names)
}

/**
 * What a session heading calls the day, beside its date: "2026-09-07 · Lower A"
 * gives "Lower A". A heading that is only a date names nothing.
 */
private fun sessionName(heading: String): String? =
    sanitise(ISO_DATE.replaceFirst(heading, "").trim().trimStart('·', '-', '—', ' '))
        .takeIf { it.isNotEmpty() }

private fun splitRow(line: String): List<String> =
    line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

private fun isSeparatorRow(cells: List<String>): Boolean =
    cells.any { it.isNotEmpty() } && cells.all { it.isEmpty() || it.all { c -> c == '-' || c == ':' } }

private fun columnIndex(header: List<String>, vararg keywords: String): Int =
    header.indexOfFirst { cell -> keywords.any { cell.contains(it) } }

private val NUMBER = Regex("""[-+]?\d*\.?\d+""")

private fun firstNumber(text: String): String? = NUMBER.find(text)?.value

private fun readExercise(header: List<String>, cells: List<String>): Exercise? {
    val nameIdx = columnIndex(header, "exercise", "movement", "lift")
    val setsIdx = columnIndex(header, "set")
    val repsIdx = columnIndex(header, "rep", "distance")
    val weightIdx = columnIndex(header, "weight", "kg", "load")

    val name = sanitise(cells.getOrNull(nameIdx).orEmpty())
    if (name.isEmpty()) return null

    val sets = cells.getOrNull(setsIdx)?.let(::firstNumber)?.toIntOrNull() ?: return null
    if (sets !in SETS_RANGE) return null

    val repsCell = cells.getOrNull(repsIdx).orEmpty()
    val reps = firstNumber(repsCell)?.toIntOrNull() ?: return null
    val isDistance = header.getOrNull(repsIdx)?.contains("distance") == true ||
        Regex("""\d\s*m\b""", RegexOption.IGNORE_CASE).containsMatchIn(repsCell)
    val unit = if (isDistance) SetUnit.METRES else SetUnit.REPS

    val kg = cells.getOrNull(weightIdx)?.let(::firstNumber)?.toFloatOrNull() ?: 0f

    return buildExercise(name, sets, reps, kg, unit)
}

/**
 * A meal needs only a name. A row that leaves its figures out — or writes them
 * as the dash an empty figure exports as — is a meal worth nothing, which is a
 * real answer while a day is still being planned.
 */
private fun readMeal(header: List<String>, cells: List<String>, id: String, date: LocalDate): Meal? {
    val nameIdx = columnIndex(header, "meal", "name")
    val detailIdx = columnIndex(header, "detail", "food", "note")
    val kcalIdx = columnIndex(header, "kcal", "calorie", "energy")
    val proteinIdx = columnIndex(header, "protein")
    val carbsIdx = columnIndex(header, "carb")

    val name = sanitise(cells.getOrNull(nameIdx).orEmpty()).take(MEAL_NAME_MAX_CHARS)
    if (name.isEmpty()) return null

    val detail = sanitise(cells.getOrNull(detailIdx).orEmpty().trim().removePrefix("—"))
        .take(MEAL_DETAIL_MAX_CHARS)

    val kcal = cells.figure(kcalIdx) ?: return null
    val protein = cells.figure(proteinIdx) ?: return null
    val carbs = cells.figure(carbsIdx) ?: return null
    if (kcal !in KCAL_RANGE || protein !in MACRO_RANGE_G || carbs !in MACRO_RANGE_G) return null

    return Meal(id, date, name, detail, kcal, protein, carbs)
}

/** A missing or dashed figure is zero; one that will not read as a number is a slip. */
private fun List<String>.figure(index: Int): Int? {
    val cell = getOrNull(index) ?: return 0
    val number = firstNumber(cell) ?: return if (cell.any { it.isDigit() }) null else 0
    return number.toFloatOrNull()?.toInt()
}

/**
 * The document as a week worth repeating. Its days become days of the week, so
 * what it plans is no longer tied to the dates it was written against — which is
 * what lets one week's plan carry into the next one, and the one after that.
 */
fun ParsedPlan.asWeekTemplate(id: String, name: String): WeekTemplate? {
    if (sessions.isEmpty()) return null
    val byDay = LinkedHashMap<DayOfWeek, TemplateSession>()
    sessions.toSortedMap().forEach { (date, exercises) ->
        // A document longer than a week names the same weekday twice. The earlier
        // day wins rather than the later one quietly overwriting it.
        byDay.putIfAbsent(
            date.dayOfWeek,
            TemplateSession(
                dayOfWeek = date.dayOfWeek,
                name = names[date] ?: date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                kind = SessionKind.STRENGTH,
                exercises = exercises,
            ),
        )
    }
    return WeekTemplate(id, name, byDay.values.sortedBy { it.dayOfWeek.value })
}
