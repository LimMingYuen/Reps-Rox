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
            // Reading through plan() is what makes the seed durable: the first write
            // folds it into the stored plan, so it stops being a fallback.
            prefs[PLAN_KEY] = encodePlan(transform(prefs.plan()).sortedBy { it.date })
        }
    }

    /** An absent key means nothing has ever been written — the one case the seed covers. */
    private fun Preferences.plan(): List<PlannedSession> = this[PLAN_KEY]?.let(::decodePlan) ?: SEED
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
    exercise.sets.joinToString(SET.toString()) { "${it.reps}$SET_FIELD${it.kg}" },
).joinToString(EXERCISE_FIELD.toString())

/**
 * Skips anything it cannot read rather than losing the whole plan to one bad
 * line — a session missing its date, kind or id is not a session.
 */
internal fun decodePlan(raw: String): List<PlannedSession> = raw.lineSequence()
    .mapNotNull(::decodeSession)
    .sortedBy { it.date }
    .toList()

private fun decodeSession(line: String): PlannedSession? {
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
    if (fields.size != 2) return null
    val reps = fields[0].toIntOrNull() ?: return null
    return WorkSet(reps, fields[1])
}

// ── Seed ────────────────────────────────────────────────────────────────────

/**
 * A first-run plan, so the week opens with the shape the design shows instead of
 * an empty list. It is the design's own week laid onto the week the app is first
 * opened in, so it reads as one whole week rather than straddling two; the first
 * session saved folds it onto disk.
 * Delete this and the fallback in [PlanRepository] to ship an empty plan.
 */
private val SEED: List<PlannedSession> by lazy {
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
