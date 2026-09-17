package com.repsrox.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "sessions")

private val LOG_KEY = stringPreferencesKey("sessions")

/**
 * Banked sessions, on disk. Same reasoning as [WeightRepository]: a session a
 * day at most, read whole every time, so one preference holding newline-separated
 * records beats a database.
 *
 * A record is `epochSecond|seconds|name|exercises`, where exercises are split by
 * `;`, an exercise reads `name:sets`, sets are split by `,`, and a set is
 * `reps*kg`, or `reps*kg*m` when the figure is metres:
 *
 * `1755302400|3492|Lower push|Back squat:5*120,5*120;Sled push:25*150*m`
 */
class SessionRepository(context: Context) {

    private val store = context.applicationContext.sessionStore

    /** Newest first — what the summary and any recent-sessions list both want. */
    val sessions: Flow<List<Session>> = store.data.map { prefs ->
        prefs[LOG_KEY]?.let(::decodeSessions).orEmpty()
    }

    suspend fun bank(session: Session) {
        store.edit { prefs ->
            val log = prefs[LOG_KEY]?.let(::decodeSessions).orEmpty()
            prefs[LOG_KEY] = encodeSessions((log + session).sortedByDescending { it.finishedAt })
        }
    }

    /** Drops [month] from the log once it has been exported. */
    suspend fun clearMonth(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()) {
        store.edit { prefs ->
            val log = prefs[LOG_KEY]?.let(::decodeSessions).orEmpty()
            prefs[LOG_KEY] = encodeSessions(sessionsWithout(month, log, zone))
        }
    }
}

// ── Codec ───────────────────────────────────────────────────────────────────

/** Separators are structure, so a name carrying one is stripped rather than allowed to break the record. */
private fun String.safe(): String = filterNot { it in "|;:,*\n" }.trim()

internal fun encodeSessions(log: List<Session>): String = log.joinToString("\n", transform = ::encodeSession)

internal fun encodeSession(session: Session): String {
    val exercises = session.exercises.joinToString(";") { exercise ->
        val sets = exercise.sets.joinToString(",") { set ->
            "${set.reps}*${set.kg.safe()}" + if (set.unit == SetUnit.METRES) "*m" else ""
        }
        "${exercise.name.safe()}:$sets"
    }
    return "${session.finishedAt.epochSecond}|${session.seconds}|${session.name.safe()}|$exercises"
}

/** Skips anything it cannot read rather than losing the whole log to one bad record. */
internal fun decodeSessions(raw: String): List<Session> = raw.lineSequence()
    .mapNotNull(::decodeSession)
    .sortedByDescending { it.finishedAt }
    .toList()

internal fun decodeSession(line: String): Session? {
    val parts = line.split('|')
    if (parts.size < 4) return null
    val epoch = parts[0].toLongOrNull() ?: return null
    val seconds = parts[1].toIntOrNull() ?: return null
    val exercises = parts[3].split(';').mapNotNull(::decodeExercise)
    // A session with nothing readable in it says nothing; drop it.
    if (exercises.isEmpty()) return null
    return Session(
        finishedAt = Instant.ofEpochSecond(epoch),
        name = parts[2],
        seconds = seconds,
        exercises = exercises,
    )
}

private fun decodeExercise(raw: String): LoggedExercise? {
    val name = raw.substringBefore(':', missingDelimiterValue = "")
    if (name.isEmpty()) return null
    val sets = raw.substringAfter(':').split(',').mapNotNull(::decodeSet)
    return if (sets.isEmpty()) null else LoggedExercise(name, sets)
}

private fun decodeSet(raw: String): WorkSet? {
    val fields = raw.split('*')
    if (fields.size < 2) return null
    val reps = fields[0].toIntOrNull() ?: return null
    if (fields[1].toFloatOrNull() == null) return null
    val unit = if (fields.getOrNull(2) == "m") SetUnit.METRES else SetUnit.REPS
    return WorkSet(reps, fields[1], unit)
}
