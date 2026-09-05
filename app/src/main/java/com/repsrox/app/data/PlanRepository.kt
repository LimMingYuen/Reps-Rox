package com.repsrox.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.planStore: DataStore<Preferences> by preferencesDataStore(name = "plan")

private val PLAN_KEY = stringPreferencesKey("sessions")

/**
 * The plan, on disk. A training block is a few dozen sessions, so the whole plan
 * lives in one preference as newline-separated records — the same bargain
 * [WeightRepository] strikes, for the same reason.
 *
 * A record is `id|date|kind|done|name|note|exercises`, where exercises are joined
 * by `;`, each `name~target~sets`, each set `repsxkg`:
 *
 *     a1|2026-08-15|STRENGTH|0|Lower push|~|Back squat~5 × 5 · 120 kg~5x120/5x120
 *
 * Nothing is seeded. A week nobody has written reads as an empty plan, which
 * the week and today screens both say plainly — a block the athlete did not
 * write is not a block they are following.
 */
class PlanRepository(context: Context) {

    private val store = context.applicationContext.planStore

    /** Earliest first — the order the week reads in. */
    val sessions: Flow<List<PlannedSession>> = store.data.map { it.plan() }

    /** Writes a session, replacing whatever was held under the same id. */
    suspend fun save(session: PlannedSession) = update { plan ->
        plan.filterNot { it.id == session.id } + session
    }

    suspend fun remove(id: String) = update { plan ->
        plan.filterNot { it.id == id }
    }

    suspend fun setDone(id: String, done: Boolean) = update { plan ->
        plan.map { if (it.id == id) it.copy(done = done) else it }
    }

    /**
     * Writes a whole plan over whatever is stored. Laying a saved week down across
     * several weeks rewrites too much of the plan to express as a set of edits.
     */
    suspend fun replaceAll(plan: List<PlannedSession>) = update { plan }

    private suspend fun update(transform: (List<PlannedSession>) -> List<PlannedSession>) {
        store.edit { prefs ->
            prefs[PLAN_KEY] = encodePlan(transform(prefs.plan()).sortedBy { it.date })
        }
    }

    /** Nothing has ever been written is an empty plan, not a seeded one. */
    private fun Preferences.plan(): List<PlannedSession> = this[PLAN_KEY]?.let(::decodePlan).orEmpty()
}

// ── Record format ───────────────────────────────────────────────────────────

internal const val FIELD = '|'
private const val EXERCISE = ';'
private const val EXERCISE_FIELD = '~'
private const val SET = '/'
private const val SET_FIELD = 'x'

/** The characters the record format claims, stripped from anything typed. */
fun sanitise(text: String): String =
    text.filterNot { it == FIELD || it == EXERCISE || it == EXERCISE_FIELD || it == '\n' || it == '\r' }
        .trim()

internal fun encodePlan(plan: List<PlannedSession>): String = plan.joinToString("\n") { session ->
    listOf(
        session.id,
        session.date.toString(),
        session.kind.name,
        if (session.done) "1" else "0",
        session.name,
        session.note,
        encodeExercises(session.exercises),
    ).joinToString(FIELD.toString())
}

/** Shared with the plan store's own records, which carry exercises the same way. */
internal fun encodeExercises(exercises: List<Exercise>): String =
    exercises.joinToString(EXERCISE.toString(), transform = ::encodeExercise)

private fun encodeExercise(exercise: Exercise): String = listOf(
    exercise.name,
    exercise.target,
    exercise.sets.joinToString(SET.toString()) { set ->
        "${set.reps}$SET_FIELD${set.kg}" + if (set.unit == SetUnit.METRES) "${SET_FIELD}m" else ""
    },
).joinToString(EXERCISE_FIELD.toString())

/**
 * Skips anything it cannot read rather than losing the whole plan to one bad
 * line — a session missing its date, kind or id is not a session.
 */
internal fun decodePlan(raw: String): List<PlannedSession> = raw.lineSequence()
    .mapNotNull(::decodePlannedSession)
    .sortedBy { it.date }
    .toList()

private fun decodePlannedSession(line: String): PlannedSession? {
    val fields = line.split(FIELD)
    if (fields.size < 7) return null
    val id = fields[0].takeIf { it.isNotBlank() } ?: return null
    val date = runCatching { LocalDate.parse(fields[1]) }.getOrNull() ?: return null
    val kind = SessionKind.entries.firstOrNull { it.name == fields[2] } ?: return null
    return PlannedSession(
        id = id,
        date = date,
        name = fields[4],
        kind = kind,
        note = fields[5],
        exercises = decodeExercises(fields[6]),
        done = fields[3] == "1",
    )
}

internal fun decodeExercises(raw: String): List<Exercise> =
    if (raw.isBlank()) emptyList() else raw.split(EXERCISE).mapNotNull(::decodeExercise)

private fun decodeExercise(raw: String): Exercise? {
    val fields = raw.split(EXERCISE_FIELD)
    if (fields.size < 3) return null
    val sets = fields[2].split(SET).mapNotNull(::decodeSet)
    // An exercise with no readable set prescribes nothing, so it is dropped whole.
    return if (sets.isEmpty()) null else Exercise(fields[0], fields[1], sets)
}

private fun decodeSet(raw: String): WorkSet? {
    val fields = raw.split(SET_FIELD)
    if (fields.size < 2) return null
    val reps = fields[0].toIntOrNull() ?: return null
    val unit = if (fields.getOrNull(2) == "m") SetUnit.METRES else SetUnit.REPS
    return WorkSet(reps, fields[1], unit)
}
