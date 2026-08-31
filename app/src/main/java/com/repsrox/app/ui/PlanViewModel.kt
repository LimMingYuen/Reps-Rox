package com.repsrox.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repsrox.app.data.ParsedPlan
import com.repsrox.app.data.PlanRepository
import com.repsrox.app.data.PlannedSession
import com.repsrox.app.data.TemplateRepository
import com.repsrox.app.data.WeekTemplate
import com.repsrox.app.data.applyTemplate
import com.repsrox.app.data.weekAsTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    /** Null until the first read off disk lands, so screens don't flash their empty state. */
    val sessions: StateFlow<List<PlannedSession>?> = repository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val plans: StateFlow<List<WeekTemplate>?> = templates.templates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── The week ────────────────────────────────────────────────────────────

    fun save(session: PlannedSession) {
        viewModelScope.launch { repository.save(session) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.remove(id) }
    }

    fun markDone(id: String) {
        viewModelScope.launch { repository.setDone(id, done = true) }
    }

    /** Applies an imported document's exercises onto whichever of today's sessions they match by date. */
    fun applyImport(parsed: ParsedPlan) {
        viewModelScope.launch {
            val current = repository.sessions.first()
            repository.replaceAll(
                current.map { session -> parsed.sessions[session.date]?.let { session.copy(exercises = it) } ?: session },
            )
        }
    }

    // ── Plans ───────────────────────────────────────────────────────────────

    /** Saves the week beginning [weekStart] as a plan that can be laid down again. */
    fun saveWeekAsPlan(weekStart: LocalDate, name: String) {
        viewModelScope.launch {
            val plan = repository.sessions.first()
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
