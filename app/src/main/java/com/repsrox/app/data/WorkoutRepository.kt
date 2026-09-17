package com.repsrox.app.data

import android.content.Context
import com.repsrox.app.data.db.AppDatabase

/** How far into a session the tracker has got: the clock, the exercise in hand, the sets banked. */
data class WorkoutProgress(
    val elapsed: Int,
    val currentExercise: Int,
    val setsDone: List<Int>,
)

/**
 * Sessions part-way through, in the database, so leaving the tracker — for
 * another session, another tab, or the app altogether — does not lose what was banked.
 */
class WorkoutRepository(context: Context) {

    private val context = context.applicationContext
    private val db = AppDatabase.get(context)
    private val dao = db.workoutDao()

    suspend fun load(id: String): WorkoutProgress? {
        LegacyImport.ensure(context, db)
        val saved = dao.load(id) ?: return null
        // A row that does not read is no progress, rather than a crash on open.
        val sets = saved.setsDone.split(',').map { it.toIntOrNull() ?: return null }
        return WorkoutProgress(saved.elapsed, saved.currentExercise, sets)
    }

    suspend fun save(id: String, progress: WorkoutProgress) {
        LegacyImport.ensure(context, db)
        dao.put(progress.toEntity(id))
    }

    /** A finished session has nothing left to pick back up. */
    suspend fun clear(id: String) {
        LegacyImport.ensure(context, db)
        dao.clear(id)
    }
}
