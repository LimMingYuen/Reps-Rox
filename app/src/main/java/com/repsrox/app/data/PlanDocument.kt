package com.repsrox.app.data

import java.time.LocalDate
import java.time.ZoneId

/**
 * The week as a Markdown document — exported so it can be edited by hand or
 * shared, and read back to update sessions' exercises. Only `## Sessions` is
 * ever imported; `## Context` is printed for reference and always ignored on
 * the way back in, however table-shaped it looks.
 */
private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")

/** How far back the Context section's training/weigh-in tables look. */
private const val CONTEXT_DAYS = 28L

private enum class Section { SESSIONS, CONTEXT, NONE }

/**
 * What a document read back. A date with no matching session on the plan it is
 * applied to is silently dropped rather than reported — there is nothing there
 * to attach an edit to.
 */
data class ParsedPlan(
    val sessions: Map<LocalDate, List<Exercise>>,
    val problems: List<String>,
) {
    val isEmpty: Boolean get() = sessions.isEmpty()

    fun summary(): String {
        if (sessions.isEmpty()) return "Nothing to apply"
        val dates = sessions.keys.sorted().joinToString(", ")
        return "${sessions.size} ${plural(sessions.size, "session")} · $dates"
    }
}

// ── Export ──────────────────────────────────────────────────────────────────

fun exportPlan(
    plan: List<PlannedSession>,
    weekStart: LocalDate,
    banked: List<Session> = emptyList(),
    weighIns: List<WeighIn> = emptyList(),
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String = buildString {
    appendLine("# RepsRox plan · week of $weekStart")
    appendLine()
    appendLine(
        "Edit the exercises below and import this text (paste it back, or open the " +
            "file) to update your sessions. Everything under Context is for reference " +
            "only and is never read back in.",
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

// ── Import ──────────────────────────────────────────────────────────────────

fun parsePlan(markdown: String): ParsedPlan {
    val sessions = mutableMapOf<LocalDate, List<Exercise>>()
    val problems = mutableListOf<String>()

    var section = Section.NONE
    var heading = ""
    var date: LocalDate? = null
    var header: List<String>? = null
    var exercises = mutableListOf<Exercise>()

    fun flush() {
        val d = date
        if (section == Section.SESSIONS && d != null && exercises.isNotEmpty()) sessions[d] = exercises.toList()
        exercises = mutableListOf()
        date = null
        header = null
    }

    markdown.lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.startsWith("## ") -> {
                flush()
                section = if (line.drop(3).trim().lowercase().contains("session")) Section.SESSIONS else Section.CONTEXT
            }
            line.startsWith("### ") && section == Section.SESSIONS -> {
                flush()
                heading = line.drop(4).trim()
                val found = ISO_DATE.find(heading)?.value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                if (found == null) problems += "No date in heading: $heading" else date = found
            }
            line.startsWith("### ") -> flush()
            section == Section.SESSIONS && date != null && line.startsWith("|") -> {
                val cells = splitRow(line)
                when {
                    isSeparatorRow(cells) -> Unit
                    header == null -> header = cells.map { it.lowercase() }
                    else -> {
                        val exercise = readExercise(header!!, cells)
                        if (exercise == null) problems += "Could not read a row under $heading" else exercises.add(exercise)
                    }
                }
            }
            else -> Unit
        }
    }
    flush()

    return ParsedPlan(sessions, problems)
}

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
