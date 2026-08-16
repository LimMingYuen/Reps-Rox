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

private fun SessionKind.shortLabel(): String = when (this) {
    SessionKind.STRENGTH -> "lift"
    SessionKind.RUN -> "run"
    SessionKind.RACE -> "race"
    SessionKind.REST -> "rest"
}
