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
 *
 * Nothing is seeded. A week nobody has written reads as an empty plan, which
 * the week and today screens both say plainly — a block the athlete did not
 * write is not a block they are following.
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

