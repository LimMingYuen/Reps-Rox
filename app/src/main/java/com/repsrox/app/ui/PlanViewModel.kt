package com.repsrox.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repsrox.app.data.ParsedPlan
import com.repsrox.app.data.PlanRepository
import com.repsrox.app.data.PlannedSession
import com.repsrox.app.data.RollingPlanRepository
import com.repsrox.app.data.TemplateRepository
import com.repsrox.app.data.WeekTemplate
import com.repsrox.app.data.applyPlanToWrittenWeeks
import com.repsrox.app.data.applyTemplate
import com.repsrox.app.data.asWeekTemplate
import com.repsrox.app.data.projectPlan
import com.repsrox.app.data.rollingWeek
import com.repsrox.app.data.weekAsTemplate
import com.repsrox.app.data.weekStart
import com.repsrox.app.data.writeDownWeek
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/**
 * The week, and the plans it can be laid down from. Kept apart from
 * [RepsRoxViewModel] for the same reason [BodyViewModel] is: both outlive the
 * process, and the live tracker's state does not.
 */
class PlanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlanRepository(application)
    private val templates = TemplateRepository(application)
    private val rolling = RollingPlanRepository(application)

    /**
     * Null until the first read off disk lands, so screens don't flash their empty
     * state. Every screen reads the week through here, so the plan in force is
     * already printed onto the weeks ahead by the time one is looked at.
     */
    val sessions: StateFlow<List<PlannedSession>?> =
        combine(repository.sessions, rolling.plan) { stored, plan ->
            projectPlan(stored, plan, LocalDate.now())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The week being repeated, if one is in force. */
    val rollingPlan: StateFlow<WeekTemplate?> = rolling.plan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val plans: StateFlow<List<WeekTemplate>?> = templates.templates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── The week ────────────────────────────────────────────────────────────

    fun save(session: PlannedSession) {
        viewModelScope.launch {
            writeDown(session.date.weekStart())
            repository.save(session)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            rollingWeek(id)?.let { writeDown(it) }
            repository.remove(id)
        }
    }

    fun markDone(id: String) {
        viewModelScope.launch {
            rollingWeek(id)?.let { writeDown(it) }
            repository.setDone(id, done = true)
        }
    }

    /**
     * Writes the week beginning [weekStart] down as it currently reads. A week the
     * plan is printing holds nothing of its own, so there is no session there to
     * edit, finish or delete; the first change to such a week makes it real first,
     * and the plan stops speaking for it from then on.
     */
    private suspend fun writeDown(weekStart: LocalDate) {
        val written = writeDownWeek(repository.sessions.first(), rolling.plan.first(), weekStart) ?: return
        repository.replaceAll(written)
    }

    /**
     * Puts an imported document in force as the plan. Its days are read as days of
     * the week, so the document plans every week from this one on rather than the
     * seven dates it happens to name — importing once is enough.
     *
     * The weeks ahead print themselves from the plan, but a week already written
     * down — this week always is, once anything in it has been built, finished or
     * seeded — would otherwise keep its old shape, and this week is the one the
     * home screen shows. So the plan is laid over every written-down week from this
     * one on as well. Weeks behind you, and sessions already banked, are left as
     * they are; a document naming exact dates still writes those days back over the
     * top, since a plan carries only one session per weekday and a longer document
     * says something different on each of its dates.
     */
    fun applyImport(parsed: ParsedPlan) {
        viewModelScope.launch {
            val plan = parsed.asWeekTemplate(id = UUID.randomUUID().toString(), name = IMPORTED)
                ?: return@launch
            rolling.set(plan)
            val written = applyPlanToWrittenWeeks(
                stored = repository.sessions.first(),
                plan = plan,
                today = LocalDate.now(),
                id = { UUID.randomUUID().toString() },
            )
            repository.replaceAll(
                written.map { session ->
                    val exercises = parsed.sessions[session.date]
                    if (session.done || exercises == null) {
                        session
                    } else {
                        session.copy(name = parsed.names[session.date] ?: session.name, exercises = exercises)
                    }
                },
            )
        }
    }

    /** Stops repeating the plan, leaving only the weeks already written down. */
    fun clearRollingPlan() {
        viewModelScope.launch { rolling.set(null) }
    }

    // ── Plans ───────────────────────────────────────────────────────────────

    /** Saves the week beginning [weekStart] as a plan that can be laid down again. */
    fun saveWeekAsPlan(weekStart: LocalDate, name: String) {
        viewModelScope.launch {
            val plan = projectPlan(repository.sessions.first(), rolling.plan.first(), LocalDate.now())
            templates.save(weekAsTemplate(plan, weekStart, name, UUID.randomUUID().toString()))
        }
    }

    fun deletePlan(id: String) {
        viewModelScope.launch { templates.remove(id) }
    }

    /**
     * Lays [planId] down over [weeks] weeks running from [weekStart]. Every week is
     * written in one pass so the plan lands whole rather than a week at a time.
     */
    fun applyPlan(planId: String, weekStart: LocalDate, weeks: Int) {
        viewModelScope.launch {
            val template = templates.templates.first().firstOrNull { it.id == planId } ?: return@launch
            var plan = repository.sessions.first()
            repeat(weeks) { week ->
                plan = applyTemplate(
                    plan = plan,
                    template = template,
                    weekStart = weekStart.plusWeeks(week.toLong()),
                    id = { UUID.randomUUID().toString() },
                )
            }
            repository.replaceAll(plan)
        }
    }
}

/** What an imported document's plan is called once it is in force. */
private const val IMPORTED = "Imported plan"
