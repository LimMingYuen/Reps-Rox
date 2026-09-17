package com.repsrox.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.repsrox.app.data.db.AppDatabase
import com.repsrox.app.data.db.MetaEntity
import com.repsrox.app.data.db.WeighInEntity
import com.repsrox.app.data.db.WorkoutProgressEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate

/*
 * Before the database, each store was one DataStore preference holding
 * newline-separated records. This file is all that is left of that: the record
 * formats, and the one-time import that carries them into Room. Once no install
 * predates the database, it and the DataStore dependency can go.
 */

private val Context.planStore: DataStore<Preferences> by preferencesDataStore(name = "plan")
private val Context.templateStore: DataStore<Preferences> by preferencesDataStore(name = "templates")
private val Context.weightStore: DataStore<Preferences> by preferencesDataStore(name = "weight")
private val Context.workoutStore: DataStore<Preferences> by preferencesDataStore(name = "workout")

private val PLAN_KEY = stringPreferencesKey("sessions")
private val TEMPLATE_KEY = stringPreferencesKey("weeks")
private val LOG_KEY = stringPreferencesKey("weigh_ins")
private val PROGRESS_KEY = stringPreferencesKey("progress")

private const val IMPORTED = "datastore_imported"

/**
 * Carries the DataStore records into the database, once. Every repository calls
 * [ensure] before it touches a table, so whichever is reached first does the work
 * and the rest wait on it.
 */
internal object LegacyImport {

    private val lock = Mutex()

    @Volatile
    private var done = false

    suspend fun ensure(context: Context, db: AppDatabase) {
        if (done) return
        lock.withLock {
            if (done) return
            if (db.metaDao().get(IMPORTED) == null) import(context.applicationContext, db)
            done = true
        }
    }

    private suspend fun import(context: Context, db: AppDatabase) {
        val stores = listOf(context.planStore, context.templateStore, context.weightStore, context.workoutStore)

        // An absent key means nothing was ever written — the one case the seeds
        // cover, exactly as they did when they were DataStore fallbacks.
        val plan = context.planStore.data.first()[PLAN_KEY]?.let(::decodePlan) ?: PLAN_SEED
        val templates = context.templateStore.data.first()[TEMPLATE_KEY]?.let(::decodeTemplates).orEmpty()
        val weighIns = context.weightStore.data.first()[LOG_KEY]?.let(::decodeLog) ?: WEIGHT_SEED
        val progress = context.workoutStore.data.first()[PROGRESS_KEY]?.let(::decodeProgress).orEmpty()

        db.withTransaction {
            db.planDao().replaceAll(plan)
            templates.forEach { db.planDao().save(it) }
            db.weightDao().putAll(weighIns.map { WeighInEntity(it.date, it.kg) })
            progress.forEach { (id, saved) -> db.workoutDao().put(saved.toEntity(id)) }
            db.metaDao().put(MetaEntity(IMPORTED, "1"))
        }

        // Only once the database holds everything; the flag above keeps a crash
        // here from importing twice.
        stores.forEach { store -> store.edit { it.clear() } }
    }
}

internal fun WorkoutProgress.toEntity(sessionId: String) =
    WorkoutProgressEntity(sessionId, elapsed, currentExercise, setsDone.joinToString(","))

// ── Record format ───────────────────────────────────────────────────────────

internal const val FIELD = '|'
internal const val EXERCISE = ';'
internal const val EXERCISE_FIELD = '~'
private const val SET = '/'
private const val SET_FIELD = 'x'

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

// ── Saved weeks ─────────────────────────────────────────────────────────────

internal fun encodeTemplates(templates: List<WeekTemplate>): String = templates
    .flatMap { template ->
        template.sessions.map { session ->
            listOf(
                template.id,
                template.name,
                session.dayOfWeek.name,
                session.kind.name,
                session.name,
                session.note,
                encodeExercises(session.exercises),
            ).joinToString(FIELD.toString())
        }
    }
    .joinToString("\n")

/**
 * Rebuilds the plans from their sessions, in the order the lines were written.
 * A line that cannot be read is dropped; a plan left with no readable session is
 * dropped with it, since an empty week is not a plan.
 */
internal fun decodeTemplates(raw: String): List<WeekTemplate> {
    val names = LinkedHashMap<String, String>()
    val sessions = LinkedHashMap<String, MutableList<TemplateSession>>()

    raw.lineSequence().forEach { line ->
        val fields = line.split(FIELD)
        if (fields.size < 7) return@forEach
        val id = fields[0].takeIf { it.isNotBlank() } ?: return@forEach
        val day = DayOfWeek.entries.firstOrNull { it.name == fields[2] } ?: return@forEach
        val kind = SessionKind.entries.firstOrNull { it.name == fields[3] } ?: return@forEach

        names.putIfAbsent(id, fields[1])
        sessions.getOrPut(id) { mutableListOf() }.add(
            TemplateSession(
                dayOfWeek = day,
                name = fields[4],
                kind = kind,
                note = fields[5],
                exercises = decodeExercises(fields[6]),
            ),
        )
    }

    return sessions.map { (id, list) -> WeekTemplate(id, names.getValue(id), list) }
}

// ── Weigh-ins ───────────────────────────────────────────────────────────────

internal fun encodeLog(log: List<WeighIn>): String = log.joinToString("\n") { entry ->
    "${entry.date}|${entry.kg}"
}

/**
 * Skips anything it cannot read rather than losing the whole log to one bad line.
 * Records carrying a third field are logs written before the waist was dropped;
 * they still read as weigh-ins, so no migration is needed.
 */
internal fun decodeLog(raw: String): List<WeighIn> = raw.lineSequence()
    .mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size < 2) return@mapNotNull null
        val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return@mapNotNull null
        val kg = parts[1].toFloatOrNull() ?: return@mapNotNull null
        WeighIn(date, kg)
    }
    .sortedBy { it.date }
    .toList()

// ── Sessions part-way through ───────────────────────────────────────────────

private const val SET_COUNT = ','

internal fun encodeProgress(progress: Map<String, WorkoutProgress>): String =
    progress.entries.joinToString("\n") { (id, it) ->
        listOf(
            id,
            it.elapsed.toString(),
            it.currentExercise.toString(),
            it.setsDone.joinToString(SET_COUNT.toString()),
        ).joinToString(FIELD.toString())
    }

/** Skips anything it cannot read, as [decodePlan] does. */
internal fun decodeProgress(raw: String): Map<String, WorkoutProgress> = raw.lineSequence()
    .mapNotNull { line ->
        val fields = line.split(FIELD)
        if (fields.size < 4) return@mapNotNull null
        val id = fields[0].takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val elapsed = fields[1].toIntOrNull() ?: return@mapNotNull null
        val current = fields[2].toIntOrNull() ?: return@mapNotNull null
        val sets = fields[3].split(SET_COUNT).map { it.toIntOrNull() ?: return@mapNotNull null }
        id to WorkoutProgress(elapsed, current, sets)
    }
    .toMap()
