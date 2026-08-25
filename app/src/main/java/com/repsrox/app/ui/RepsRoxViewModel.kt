package com.repsrox.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repsrox.app.data.EXERCISES
import com.repsrox.app.data.Exercise
import com.repsrox.app.data.LEGS
import com.repsrox.app.data.LIVE_ELAPSED
import com.repsrox.app.data.LIVE_SESSION
import com.repsrox.app.data.LoggedExercise
import com.repsrox.app.data.MEALS
import com.repsrox.app.data.PlannedSession
import com.repsrox.app.data.RUN_SECONDS_PER_KM
import com.repsrox.app.data.Session
import com.repsrox.app.data.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Locale

/**
 * Everything the design's script block kept in component state. Initial values
 * are the design's, so the app opens mid-week with a session part-logged.
 *
 * The strength session is the one thing here that outlives the process: finishing
 * it writes it to [SessionRepository] and the summary reads it back. The run,
 * the race and the fuel check-ins are still the design's fixed sample content.
 */
class RepsRoxViewModel(application: Application) : AndroidViewModel(application) {

    private val sessions = SessionRepository(application)

    var screen by mutableStateOf(Screen.Today)
        private set

    /**
     * The session being tracked, once one has been opened off the plan. Null until
     * then, which is when the screens fall back to the design's own content.
     */
    var activeSession by mutableStateOf<PlannedSession?>(null)
        private set

    /** The week being looked at, shared by the week screen and the plan shelf. */
    var weekStart by mutableStateOf(LocalDate.now().weekStart())
        private set

    /** The day a new session is opened against — whichever one was tapped. */
    var buildDate by mutableStateOf(LocalDate.now())
        private set

    /** Sets banked per exercise, indexed alongside [activeExercises]. */
    val setsDone = mutableStateListOf(2, 0, 0, 0, 0)

    var currentExercise by mutableIntStateOf(0)
        private set

    var restSeconds by mutableIntStateOf(96)
        private set

    /** The live session's own clock. Seeded mid-session, as the design opens. */
    var sessionSeconds by mutableIntStateOf(LIVE_ELAPSED)
        private set

    /** False once the session has been banked, until the next one is started. */
    var sessionLive by mutableStateOf(true)
        private set

    var runSeconds by mutableIntStateOf(2498)
        private set

    var runOn by mutableStateOf(true)
        private set

    var raceSeconds by mutableIntStateOf(2874)
        private set

    var raceOn by mutableStateOf(true)
        private set

    var legIndex by mutableIntStateOf(5)
        private set

    /** Keys of the meals checked off today. */
    val mealsLogged = listOf("b", "l").toMutableStateList()

    /** Which banked session the summary shows. Null means the newest one. */
    var viewedSession by mutableStateOf<Instant?>(null)
        private set

    /** Null until the first read off disk lands, so the summary does not flash its empty state. */
    val bankedSessions: StateFlow<List<Session>?> = sessions.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Derived ─────────────────────────────────────────────────────────────

    /**
     * What the live tracker works through. A session with no exercises of its own
     * — a run, or one of the design's own week rows — falls back to the design's
     * session rather than opening a tracker with nothing in it.
     */
    val activeExercises: List<Exercise>
        get() = activeSession?.exercises?.takeIf { it.isNotEmpty() } ?: EXERCISES

    val plannedSets: Int get() = activeExercises.sumOf { it.sets.size }

    val totalSetsDone: Int get() = setsDone.sum()

    val leg get() = LEGS[legIndex]

    /** Stations reached so far — the number the race ring and dial show. */
    val stationNumber: Int
        get() = LEGS.take(legIndex + 1).count { it.isStation }

    /** Fixed to [Locale.US] so the decimal separator matches the rest of the design. */
    val runKilometres: String
        get() = String.format(Locale.US, "%.2f", runSeconds / RUN_SECONDS_PER_KM)

    /** The exercises as actually worked, which is what gets banked. */
    private val bankedExercises: List<LoggedExercise>
        get() = EXERCISES.mapIndexedNotNull { index, exercise ->
            val done = setsDone[index]
            if (done == 0) null else LoggedExercise(exercise.name, exercise.sets.take(done))
        }

    // ── Intents ─────────────────────────────────────────────────────────────

    fun go(destination: Screen) {
        // Walking back into the live screen after banking one starts the next.
        if (destination == Screen.Live && !sessionLive) startSession()
        // Every route into the summary but picking a session shows the newest.
        if (destination == Screen.Summary) viewedSession = null
        screen = destination
    }

    /** Opens one particular banked session, rather than whichever is newest. */
    fun openSession(session: Session) {
        viewedSession = session.finishedAt
        screen = Screen.Summary
    }

    fun back(): Boolean {
        val parent = screen.parent() ?: return false
        screen = parent
        return true
    }

    /**
     * Tapping a set banks every set up to it; tapping the last banked set gives
     * it back. Either way the rest clock restarts.
     */
    fun logSet(exercise: Int, index: Int) {
        setsDone[exercise] = if (setsDone[exercise] == index + 1) index else index + 1
        restSeconds = 105
    }

    fun selectExercise(index: Int) {
        currentExercise = index
    }

    fun skipRest() {
        restSeconds = 0
    }

    /**
     * Writes the session to disk and opens its summary. A session with nothing
     * banked in it is worth no record, so it just ends.
     */
    fun finishSession() {
        val worked = bankedExercises
        if (worked.isNotEmpty()) {
            val session = Session(
                finishedAt = Instant.now(),
                name = LIVE_SESSION.name,
                seconds = sessionSeconds,
                exercises = worked,
            )
            viewModelScope.launch { sessions.bank(session) }
        }
        sessionLive = false
        go(Screen.Summary)
    }

    /** Clears the board for the next session. The clock starts at zero, not mid-session. */
    private fun startSession() {
        setsDone.indices.forEach { setsDone[it] = 0 }
        currentExercise = 0
        restSeconds = 0
        sessionSeconds = 0
        sessionLive = true
    }

    fun toggleRun() {
        runOn = !runOn
    }

    fun toggleRace() {
        raceOn = !raceOn
    }

    fun nextLeg() {
        legIndex = (legIndex + 1).coerceAtMost(LEGS.lastIndex)
    }

    fun toggleMeal(key: String) {
        if (!mealsLogged.remove(key)) mealsLogged.add(key)
    }

    fun isMealLogged(key: String) = key in mealsLogged

    /** One second of wall clock: rest counts down, the timers count up. */
    fun tick() {
        if (restSeconds > 0) restSeconds--
        if (sessionLive) sessionSeconds++
        if (runOn) runSeconds++
        if (raceOn) raceSeconds++
    }

    init {
        require(setsDone.size == EXERCISES.size) { "setsDone must cover every exercise" }
        require(MEALS.map { it.key }.containsAll(mealsLogged)) { "unknown meal key" }
    }
}

/** m:ss — the design's short clock. */
fun formatMinutes(total: Int): String = "${total / 60}:${(total % 60).toString().padStart(2, '0')}"

/** h:mm:ss — the race clock. */
fun formatHours(total: Int): String {
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
