package com.repsrox.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.repsrox.app.data.Exercise
import com.repsrox.app.data.PlannedSession
import com.repsrox.app.data.SessionKind
import com.repsrox.app.data.SetUnit
import com.repsrox.app.data.TemplateSession
import com.repsrox.app.data.WeekTemplate
import com.repsrox.app.data.WorkSet
import java.time.DayOfWeek
import java.time.LocalDate

// ── Tables ──────────────────────────────────────────────────────────────────

@Entity(tableName = "sessions", indices = [Index("date")])
data class SessionEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val kind: SessionKind,
    val done: Boolean,
    val name: String,
    val note: String,
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
)

@Entity(
    tableName = "template_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("templateId")],
)
data class TemplateSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: String,
    val position: Int,
    val dayOfWeek: DayOfWeek,
    val kind: SessionKind,
    val name: String,
    val note: String,
)

/**
 * An exercise belongs to a planned session or to a session in a saved week —
 * exactly one of the two owners is set. Either way it goes when its owner does.
 */
@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TemplateSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateSessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("templateSessionId")],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String? = null,
    val templateSessionId: Long? = null,
    val position: Int,
    val name: String,
    val target: String,
)

@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId")],
)
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val position: Int,
    val reps: Int,
    /** Text, as [WorkSet] holds it: blank is bodyweight. */
    val kg: String,
    /** Whether [reps] counts reps or metres — a run or a carry is a distance. */
    @ColumnInfo(defaultValue = "REPS") val unit: SetUnit = SetUnit.REPS,
)

@Entity(tableName = "weigh_ins")
data class WeighInEntity(
    /** One weigh-in a day, so the date is the key and a second entry replaces the first. */
    @PrimaryKey val date: LocalDate,
    val kg: Float,
)

/**
 * A session part-way through. Not tied to `sessions` by a foreign key: laying a
 * plan down rewrites the sessions table, and that must not take progress with it.
 */
@Entity(tableName = "workout_progress")
data class WorkoutProgressEntity(
    @PrimaryKey val sessionId: String,
    val elapsed: Int,
    val currentExercise: Int,
    /** Sets banked per exercise, comma-joined — tracker state, never queried into. */
    val setsDone: String,
)

/** Bookkeeping, such as whether the DataStore import has run. */
@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

// ── Relations ───────────────────────────────────────────────────────────────

data class ExerciseWithSets(
    @Embedded val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val sets: List<SetEntity>,
)

data class SessionWithExercises(
    @Embedded val session: SessionEntity,
    @Relation(entity = ExerciseEntity::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<ExerciseWithSets>,
)

data class TemplateSessionWithExercises(
    @Embedded val session: TemplateSessionEntity,
    @Relation(entity = ExerciseEntity::class, parentColumn = "id", entityColumn = "templateSessionId")
    val exercises: List<ExerciseWithSets>,
)

data class TemplateWithSessions(
    @Embedded val template: TemplateEntity,
    @Relation(entity = TemplateSessionEntity::class, parentColumn = "id", entityColumn = "templateId")
    val sessions: List<TemplateSessionWithExercises>,
)

// ── To the models the rest of the app speaks ────────────────────────────────

// Relations come back in no promised order, so every list is put back by position.

private fun List<ExerciseWithSets>.toModels(): List<Exercise> = sortedBy { it.exercise.position }.map { row ->
    Exercise(
        name = row.exercise.name,
        target = row.exercise.target,
        sets = row.sets.sortedBy { it.position }.map { WorkSet(it.reps, it.kg, it.unit) },
    )
}

fun SessionWithExercises.toModel() = PlannedSession(
    id = session.id,
    date = session.date,
    name = session.name,
    kind = session.kind,
    note = session.note,
    exercises = exercises.toModels(),
    done = session.done,
)

fun TemplateWithSessions.toModel() = WeekTemplate(
    id = template.id,
    name = template.name,
    sessions = sessions.sortedBy { it.session.position }.map { row ->
        TemplateSession(
            dayOfWeek = row.session.dayOfWeek,
            name = row.session.name,
            kind = row.session.kind,
            note = row.session.note,
            exercises = row.exercises.toModels(),
        )
    },
)

fun PlannedSession.toEntity() = SessionEntity(id, date, kind, done, name, note)
