package com.repsrox.app.data

import android.content.Context
import com.repsrox.app.data.db.AppDatabase
import com.repsrox.app.data.db.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * The plan, in the database: a row per session, its exercises and their sets in
 * tables of their own, so the plan can be asked questions rather than only read whole.
 */
class PlanRepository(context: Context) {

    private val context = context.applicationContext
    private val db = AppDatabase.get(context)
    private val dao = db.planDao()

    /** Earliest first — the order the week reads in. */
    val sessions: Flow<List<PlannedSession>> = flow {
        LegacyImport.ensure(this@PlanRepository.context, db)
        emitAll(dao.observeSessions().map { rows -> rows.map { it.toModel() } })
    }

    /** Writes a session, replacing whatever was held under the same id. */
    suspend fun save(session: PlannedSession) = write { dao.save(session) }

    suspend fun remove(id: String) = write { dao.remove(id) }

    suspend fun setDone(id: String, done: Boolean) = write { dao.setDone(id, done) }

    /**
     * Writes a whole plan over whatever is stored. Laying a saved week down across
     * several weeks rewrites too much of the plan to express as a set of edits.
     */
    suspend fun replaceAll(plan: List<PlannedSession>) = write { dao.replaceAll(plan.sortedBy { it.date }) }

    private suspend fun write(block: suspend () -> Unit) {
        LegacyImport.ensure(context, db)
        block()
    }
}

/** The characters the record format claims, stripped from anything typed. */
fun sanitise(text: String): String =
    text.filterNot { it == FIELD || it == EXERCISE || it == EXERCISE_FIELD || it == '\n' || it == '\r' }
        .trim()

// ── Seed ────────────────────────────────────────────────────────────────────

/**
 * A first-run plan, so the week opens with the shape the design shows instead of
 * an empty list. It is the design's own week laid onto the week the app is first
 * opened in, so it reads as one whole week rather than straddling two. It is
 * written into the database when that is first set up.
 * Delete this and its use in [LegacyImport] to ship an empty plan.
 */
internal val PLAN_SEED: List<PlannedSession> by lazy {
    val today = LocalDate.now()
    val monday = today.weekStart()
    WEEK_TEMPLATE.mapIndexed { index, seed ->
        val date = monday.plusDays((seed.dayOfWeek.value - 1).toLong())
        PlannedSession(
            id = "seed-$index",
            date = date,
            name = seed.name,
            kind = seed.kind,
            note = seed.note,
            exercises = seed.exercises,
            // The days already behind you read as banked; the rest are still ahead.
            done = date.isBefore(today) && seed.kind != SessionKind.REST,
        )
    }
}
