package com.repsrox.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * One session in a saved week, held against the day it falls on rather than a
 * date — that is what makes the week reusable.
 */
data class TemplateSession(
    val dayOfWeek: DayOfWeek,
    val name: String,
    val kind: SessionKind,
    val note: String = "",
    val exercises: List<Exercise> = emptyList(),
)

/**
 * A week worth repeating, saved under a name. The app calls these plans, which is
 * what they are to the person training: the shape of a normal week, applied to
 * whichever weeks you want it on.
 */
data class WeekTemplate(
    val id: String,
    val name: String,
    val sessions: List<TemplateSession>,
) {
    val training: List<TemplateSession> get() = sessions.filter { it.kind != SessionKind.REST }

    /** "5 sessions · 3 lift, 2 run" — what the plan asks of a week, at a glance. */
    fun summary(): String {
        val counts = training.groupingBy { it.kind }.eachCount()
        val parts = SessionKind.entries.mapNotNull { kind ->
            counts[kind]?.let { "$it ${kind.shortLabel()}" }
        }
        val total = training.size
        return listOf("$total ${plural(total, "session")}", parts.joinToString(", "))
            .filter { it.isNotBlank() }
            .joinToString(" · ")
    }
}

/** Monday of the week [this] falls in — the day every week here starts on. */
fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** The seven dates of the week beginning [this]. */
fun LocalDate.weekDates(): List<LocalDate> = (0L..6L).map { plusDays(it) }

/** Turns a saved week into real dated sessions, on the week beginning [weekStart]. */
fun WeekTemplate.materialise(weekStart: LocalDate, id: (Int) -> String): List<PlannedSession> =
    sessions.mapIndexed { index, session ->
        PlannedSession(
            id = id(index),
            // The template's Monday is the week's Monday, whatever date that is.
            date = weekStart.plusDays((session.dayOfWeek.value - 1).toLong()),
            name = session.name,
            kind = session.kind,
            note = session.note,
            exercises = session.exercises,
        )
    }

/**
 * Applies [template] to the week beginning [weekStart], replacing what was planned
 * there. Sessions already finished are left alone — the plan is what is still
 * ahead of you, and a week's record is not something re-planning should erase.
 * A day already carrying a finished session is skipped rather than doubled up.
 */
fun applyTemplate(
    plan: List<PlannedSession>,
    template: WeekTemplate,
    weekStart: LocalDate,
    id: (Int) -> String,
): List<PlannedSession> {
    val week = weekStart.weekDates().toSet()
    val kept = plan.filter { it.date !in week || it.done }
    val spokenFor = kept.filter { it.date in week }.map { it.date }.toSet()

    val fresh = template.materialise(weekStart, id).filterNot { it.date in spokenFor }
    return (kept + fresh).sortedBy { it.date }
}

/**
 * Saves the sessions of one week as a plan. Whether a session was finished is not
 * part of the shape of a week, so it is left behind.
 */
fun weekAsTemplate(
    plan: List<PlannedSession>,
    weekStart: LocalDate,
    name: String,
    id: String,
): WeekTemplate {
    val week = weekStart.weekDates().toSet()
    return WeekTemplate(
        id = id,
        name = name,
        sessions = plan.filter { it.date in week }
            .sortedBy { it.date }
            .map { session ->
                TemplateSession(
                    dayOfWeek = session.date.dayOfWeek,
                    name = session.name,
                    kind = session.kind,
                    note = session.note,
                    exercises = session.exercises,
                )
            },
    )
}

/** The count of weeks a plan can be laid down for in one go. */
val REPEAT_RANGE = 1..12

// ── The rolling plan ───────────────────────────────────────────────────────

/**
 * One plan can be the shape of every week from here on rather than a week you
 * lay down again each Monday. Weeks it fills are not written to disk — they are
 * worked out from the plan whenever the week is read — so changing the plan
 * changes every week ahead at once.
 *
 * A week that holds a stored session of its own is that week's own business and
 * the plan leaves it alone whole. That is what makes a week editable: the first
 * edit writes the week down (see the fork in `PlanViewModel`), and from then on
 * it is a real week rather than a printing of the plan.
 */
const val ROLLING_PREFIX = "rolling"

/** How far ahead the plan is worked out. Past a year, a plan is a different plan. */
const val ROLLING_HORIZON_WEEKS = 52

/** The id a session printed from the plan carries — the week it fell in, and where in it. */
fun rollingId(weekStart: LocalDate, index: Int): String = "$ROLLING_PREFIX-$weekStart-$index"

/** The week a printed session belongs to, or null for one that was written down. */
fun rollingWeek(id: String): LocalDate? {
    if (!id.startsWith("$ROLLING_PREFIX-")) return null
    val date = id.removePrefix("$ROLLING_PREFIX-").substringBeforeLast("-")
    return runCatching { LocalDate.parse(date) }.getOrNull()
}

/**
 * The plan as the app reads it: what is stored, plus [rolling] printed onto every
 * week from [today]'s on that holds nothing of its own. Weeks behind you are left
 * as they were — a plan says what you will do, and cannot say what you did.
 */
fun projectPlan(
    stored: List<PlannedSession>,
    rolling: WeekTemplate?,
    today: LocalDate,
    weeks: Int = ROLLING_HORIZON_WEEKS,
): List<PlannedSession> {
    if (rolling == null || rolling.sessions.isEmpty()) return stored
    val spokenFor = stored.mapTo(mutableSetOf()) { it.date.weekStart() }
    val start = today.weekStart()
    val printed = (0 until weeks)
        .map { start.plusWeeks(it.toLong()) }
        .filterNot { it in spokenFor }
        .flatMap { week -> rolling.materialise(week) { index -> rollingId(week, index) } }
    return (stored + printed).sortedBy { it.date }
}

/**
 * The week beginning [weekStart] written down as the plan currently prints it, or
 * null when there is nothing to write: no plan is in force, or the week already
 * holds sessions of its own and is nobody's business but its own.
 *
 * This is what a week has to go through before anything in it can be edited,
 * finished or deleted — until then its sessions are printed rather than stored,
 * and there is nothing on disk to change.
 */
fun writeDownWeek(
    stored: List<PlannedSession>,
    rolling: WeekTemplate?,
    weekStart: LocalDate,
): List<PlannedSession>? {
    if (rolling == null || rolling.sessions.isEmpty()) return null
    if (stored.any { it.date.weekStart() == weekStart }) return null
    return (stored + rolling.materialise(weekStart) { rollingId(weekStart, it) }).sortedBy { it.date }
}

/**
 * The weeks from [from]'s up to, but not including, [today]'s, written down as the
 * plan printed them — or null when there is nothing to write.
 *
 * A printed week is worked out at read time and never stored, so the Monday after
 * it stops being printed there is nothing left of it: a week you planned and never
 * touched would simply be gone, and the month's export short those sessions.
 * Settled sessions are unfinished, since none of them was ever marked done.
 *
 * Weeks holding sessions of their own are already their own business, and are
 * left alone exactly as [projectPlan] leaves them.
 */
fun settleWeeks(
    stored: List<PlannedSession>,
    rolling: WeekTemplate?,
    from: LocalDate,
    today: LocalDate,
): List<PlannedSession>? {
    if (rolling == null || rolling.sessions.isEmpty()) return null
    val start = from.weekStart()
    val end = today.weekStart()
    if (!start.isBefore(end)) return null
    val spokenFor = stored.mapTo(mutableSetOf()) { it.date.weekStart() }
    val printed = generateSequence(start) { it.plusWeeks(1) }
        .takeWhile { it.isBefore(end) }
        .filterNot { it in spokenFor }
        .flatMap { week -> rolling.materialise(week) { index -> rollingId(week, index) } }
        .toList()
    if (printed.isEmpty()) return null
    return (stored + printed).sortedBy { it.date }
}

/**
 * Lays [plan] over the weeks already written down — this week's and any ahead of
 * it. Weeks holding nothing of their own need no help: the plan prints onto them
 * itself. The week under way does, and that is the week being looked at, so a plan
 * that changed every week but this one would read as not having landed at all.
 * Weeks behind you, and sessions already finished, are left as they are.
 */
fun applyPlanToWrittenWeeks(
    stored: List<PlannedSession>,
    plan: WeekTemplate,
    today: LocalDate,
    id: (Int) -> String,
): List<PlannedSession> {
    val from = today.weekStart()
    // Builds before the empty first run seeded a sample week and marked its days
    // done. Nobody trained those, so they are not a record for the plan to step
    // around — left in, a seeded Monday keeps the imported Monday off the week.
    val own = stored.filterNot { it.isLeftoverSeed() && !it.date.isBefore(from) }
    return stored.map { it.date.weekStart() }
        .filterNot { it.isBefore(from) }
        .distinct()
        .fold(own) { written, week -> applyTemplate(written, plan, week, id) }
}

/**
 * The plan with [before] — an exercise in [session] — replaced by [after], so a
 * change made to it on the day carries on into every week the plan prints from
 * here. Null when there is nothing to carry: the session's weekday has no match in
 * the plan, the plan holds no such exercise there, or it already reads as [after].
 *
 * A session printed from the plan says where in it it came from; one written down
 * since (an import lays fresh ids over written weeks) is found by its weekday. The
 * exercise is found by the name it had before the change, at its own position when
 * that still lines up.
 */
fun WeekTemplate.carryForward(
    session: PlannedSession,
    exerciseIndex: Int,
    before: Exercise,
    after: Exercise,
): WeekTemplate? {
    val day = session.date.dayOfWeek
    val printedAt = session.id.takeIf { rollingWeek(it) != null }
        ?.substringAfterLast("-")?.toIntOrNull()
        ?.takeIf { sessions.getOrNull(it)?.dayOfWeek == day }
    val slot = printedAt
        ?: sessions.indexOfFirst { it.dayOfWeek == day && it.kind == session.kind }.takeIf { it >= 0 }
        ?: return null
    val exercises = sessions[slot].exercises
    val at = exerciseIndex.takeIf { exercises.getOrNull(it)?.name == before.name }
        ?: exercises.indexOfFirst { it.name == before.name }.takeIf { it >= 0 }
        ?: return null
    if (exercises[at] == after) return null
    val changed = sessions[slot].copy(exercises = exercises.toMutableList().also { it[at] = after })
    return copy(sessions = sessions.toMutableList().also { it[slot] = changed })
}

/** The id the sample week's sessions were written under, back when one was seeded. */
private const val SEED_PREFIX = "seed-"

private fun PlannedSession.isLeftoverSeed(): Boolean = id.startsWith(SEED_PREFIX)

private fun SessionKind.shortLabel(): String = when (this) {
    SessionKind.STRENGTH -> "lift"
    SessionKind.RUN -> "run"
    SessionKind.RACE -> "race"
    SessionKind.REST -> "rest"
}
