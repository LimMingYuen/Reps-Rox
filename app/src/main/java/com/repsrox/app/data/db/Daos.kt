package com.repsrox.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.repsrox.app.data.Exercise
import com.repsrox.app.data.PlannedSession
import com.repsrox.app.data.WeekTemplate
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** The week and the saved weeks, which share the exercise and set tables. */
@Dao
abstract class PlanDao {

    // ── The week ────────────────────────────────────────────────────────────

    /** Earliest first; sessions on one day stay in the order they were written. */
    @Transaction
    @Query("SELECT * FROM sessions ORDER BY date, rowid")
    abstract fun observeSessions(): Flow<List<SessionWithExercises>>

    /** Writes a session whole, replacing whatever was held under the same id. */
    @Transaction
    open suspend fun save(session: PlannedSession) {
        deleteSession(session.id)
        insertWhole(session)
    }

    @Transaction
    open suspend fun remove(id: String) {
        deleteSession(id)
        deleteOrphanedProgress()
    }

    @Query("UPDATE sessions SET done = :done WHERE id = :id")
    abstract suspend fun setDone(id: String, done: Boolean)

    @Transaction
    open suspend fun replaceAll(plan: List<PlannedSession>) {
        deleteAllSessions()
        plan.forEach { insertWhole(it) }
        deleteOrphanedProgress()
    }

    private suspend fun insertWhole(session: PlannedSession) {
        insert(session.toEntity())
        insertExercises(session.exercises) { position, exercise ->
            ExerciseEntity(
                sessionId = session.id,
                position = position,
                name = exercise.name,
                target = exercise.target,
            )
        }
    }

    // ── Saved weeks ─────────────────────────────────────────────────────────

    /** In the order they were saved. */
    @Transaction
    @Query("SELECT * FROM templates ORDER BY rowid")
    abstract fun observeTemplates(): Flow<List<TemplateWithSessions>>

    @Transaction
    open suspend fun save(template: WeekTemplate) {
        deleteTemplate(template.id)
        insert(TemplateEntity(template.id, template.name))
        template.sessions.forEachIndexed { position, session ->
            val sessionId = insert(
                TemplateSessionEntity(
                    templateId = template.id,
                    position = position,
                    dayOfWeek = session.dayOfWeek,
                    kind = session.kind,
                    name = session.name,
                    note = session.note,
                ),
            )
            insertExercises(session.exercises) { index, exercise ->
                ExerciseEntity(
                    templateSessionId = sessionId,
                    position = index,
                    name = exercise.name,
                    target = exercise.target,
                )
            }
        }
    }

    @Query("DELETE FROM templates WHERE id = :id")
    abstract suspend fun deleteTemplate(id: String)

    // ── Rows ────────────────────────────────────────────────────────────────

    private suspend fun insertExercises(
        exercises: List<Exercise>,
        row: (position: Int, exercise: Exercise) -> ExerciseEntity,
    ) {
        exercises.forEachIndexed { position, exercise ->
            val exerciseId = insert(row(position, exercise))
            insertSets(
                exercise.sets.mapIndexed { index, set ->
                    SetEntity(exerciseId = exerciseId, position = index, reps = set.reps, kg = set.kg)
                },
            )
        }
    }

    @Insert
    protected abstract suspend fun insert(session: SessionEntity)

    @Insert
    protected abstract suspend fun insert(template: TemplateEntity)

    @Insert
    protected abstract suspend fun insert(session: TemplateSessionEntity): Long

    @Insert
    protected abstract suspend fun insert(exercise: ExerciseEntity): Long

    @Insert
    protected abstract suspend fun insertSets(sets: List<SetEntity>)

    @Query("DELETE FROM sessions WHERE id = :id")
    protected abstract suspend fun deleteSession(id: String)

    @Query("DELETE FROM sessions")
    protected abstract suspend fun deleteAllSessions()

    @Query("DELETE FROM workout_progress WHERE sessionId NOT IN (SELECT id FROM sessions)")
    protected abstract suspend fun deleteOrphanedProgress()
}

@Dao
interface WeightDao {

    /** Oldest first — the order both the chart and the trend want. */
    @Query("SELECT * FROM weigh_ins ORDER BY date")
    fun observe(): Flow<List<WeighInEntity>>

    /** The date is the key, so a second weigh-in on a day replaces the first. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: WeighInEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putAll(entries: List<WeighInEntity>)

    @Query("DELETE FROM weigh_ins WHERE date = :date")
    suspend fun remove(date: LocalDate)
}

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workout_progress WHERE sessionId = :sessionId")
    suspend fun load(sessionId: String): WorkoutProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(progress: WorkoutProgressEntity)

    @Query("DELETE FROM workout_progress WHERE sessionId = :sessionId")
    suspend fun clear(sessionId: String)
}

@Dao
interface MetaDao {

    @Query("SELECT value FROM meta WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: MetaEntity)
}
