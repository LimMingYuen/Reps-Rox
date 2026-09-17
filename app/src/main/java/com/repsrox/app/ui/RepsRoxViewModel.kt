package com.repsrox.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repsrox.app.data.Exercise
import com.repsrox.app.data.LEGS
import com.repsrox.app.data.LoggedExercise
import com.repsrox.app.data.PlanRepository
import com.repsrox.app.data.PlannedSession
import com.repsrox.app.data.RUN_SECONDS_PER_KM
import com.repsrox.app.data.Session
import com.repsrox.app.data.SessionKind
import com.repsrox.app.data.SessionRepository
import com.repsrox.app.data.WorkoutProgress
import com.repsrox.app.data.WorkoutRepository
import com.repsrox.app.data.formatMinutes
import com.repsrox.app.data.weekStart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

/**
 * Everything the design's script block kept in component state. Every clock
 * here starts at zero and stopped: opening a board is not starting the work on
 * it, which is what the Start button is for.
 *
 * The strength session is the one thing here that outlives the process: finishing
 * it writes it to [SessionRepository] and the summary reads it back. The run and
 * the race hold their splits only as long as the process lives, but a session
 * killed mid-way — the process, not the athlete — picks back up where the
 * tracker last saved it, through [WorkoutRepository].
 */
class RepsRoxViewModel(application: Application) : AndroidViewModel(application) {

    private val sessions = SessionRepository(application)
    private val plans = PlanRepository(application)
    private val workouts = WorkoutRepository(application)

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

    /** The session the builder is editing, or null when it's building a fresh one. */
    var editingSession by mutableStateOf<PlannedSession?>(null)
        private set

    /** Sets banked per exercise, indexed alongside [activeExercises]. Sized by [open]. */
    val setsDone = mutableStateListOf<Int>()

    var currentExercise by mutableIntStateOf(0)
        private set

    var restSeconds by mutableIntStateOf(0)
        private set

    /** The live session's own clock, from the moment a session is opened. */
    var sessionSeconds by mutableIntStateOf(0)
        private set

    /** True only while a session is being worked. Opening the board starts one. */
    var sessionLive by mutableStateOf(false)
        private set

    /** The run's own clock. Starts at zero and stopped: opening the board is not running. */
    var runSeconds by mutableIntStateOf(0)
        private set

    var runOn by mutableStateOf(false)
        private set

    /** The sim's own clock, on the same terms as the run's — Start begins it, not arriving. */
    var raceSeconds by mutableIntStateOf(0)
        private set

    var raceOn by mutableStateOf(false)
        private set

    /** Kilometres closed, in the order they were run. What the splits table shows. */
    val runSplits = mutableStateListOf<Int>()

    /** Legs closed, in course order, each at whatever the clock said it took. */
    val legSeconds = mutableStateListOf<Int>()

    /** Which banked session the summary shows. Null means the newest one. */
    var viewedSession by mutableStateOf<Instant?>(null)
        private set

    /** Null until the first read off disk lands, so the summary does not flash its empty state. */
    val bankedSessions: StateFlow<List<Session>?> = sessions.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Derived ─────────────────────────────────────────────────────────────

    /** What the live tracker works through — the opened session's own board. */
    val activeExercises: List<Exercise>
        get() = activeSession?.exercises.orEmpty()

    val plannedSets: Int get() = activeExercises.sumOf { it.sets.size }

    val totalSetsDone: Int get() = setsDone.sum()

    /**
     * The leg being raced: the one after everything closed. The last leg holds
     * once it is closed, so a finished sim has somewhere to sit.
     */
    val legIndex: Int get() = legSeconds.size.coerceAtMost(LEGS.lastIndex)

    val leg get() = LEGS[legIndex]

    /** True once every leg has been closed. */
    val raceFinished: Boolean get() = legSeconds.size == LEGS.size

    /** The clock on the leg in progress — the race clock less everything banked. */
    val currentLegSeconds: Int get() = raceSeconds - legSeconds.sum()

    /** The clock on the kilometre in progress. */
    val currentLapSeconds: Int get() = runSeconds - runSplits.sum()

    /** Average kilometre so far. Null until one has been closed. */
    val runPace: String?
        get() = if (runSplits.isEmpty()) null else formatMinutes(runSplits.sum() / runSplits.size)

    /** Stations reached so far — the number the race ring and dial show. */
    val stationNumber: Int
        get() = LEGS.take(legIndex + 1).count { it.isStation }

    /** Fixed to [Locale.US] so the decimal separator matches the rest of the design. */
    val runKilometres: String
        get() = String.format(Locale.US, "%.2f", runSeconds / RUN_SECONDS_PER_KM)

    /** The exercises as actually worked, which is what gets banked. */
    private val bankedExercises: List<LoggedExercise>
        get() = activeExercises.mapIndexedNotNull { index, exercise ->
            val done = setsDone.getOrElse(index) { 0 }
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
        persist()
    }

    fun selectExercise(index: Int) {
        currentExercise = index
        persist()
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
        val opened = activeSession
        if (worked.isNotEmpty()) {
            val session = Session(
                finishedAt = Instant.now(),
                name = opened?.name ?: "Session",
                seconds = sessionSeconds,
                exercises = worked,
            )
            viewModelScope.launch { sessions.bank(session) }
            if (opened != null) viewModelScope.launch { plans.setDone(opened.id, done = true) }
        }
        // Banked or not, the session is over — nothing left here to resume.
        opened?.let { session -> viewModelScope.launch { workouts.clear(session.id) } }
        sessionLive = false
        go(Screen.Summary)
    }

    /** Ends a run or race: marks its plan session done and drops whatever was saved to resume it. */
    fun endTracked() {
        val opened = activeSession
        runOn = false
        raceOn = false
        if (opened != null) {
            viewModelScope.launch { workouts.clear(opened.id) }
            viewModelScope.launch { plans.setDone(opened.id, done = true) }
        }
        go(Screen.Summary)
    }

    /**
     * Opens a session off the plan — what the live tracker (or run/race screen)
     * works through. Reopening the session already being tracked keeps whatever
     * has been banked into it; a different one picks up whatever was saved
     * against it, or starts clean.
     */
    fun open(session: PlannedSession) {
        if (session.id != activeSession?.id) persist()
        activeSession = session
        setsDone.clear()
        setsDone.addAll(List(session.exercises.size) { 0 })
        startSession()
        startRun()
        startRace()
        if (!session.done) restore(session.id, session.exercises)
        go(
            when (session.kind) {
                SessionKind.RUN -> Screen.Run
                SessionKind.RACE -> Screen.Race
                SessionKind.STRENGTH, SessionKind.REST -> Screen.Live
            },
        )
    }

    /** Moves the week screen to the week beginning [date]. */
    fun goToWeek(date: LocalDate) {
        weekStart = date.weekStart()
    }

    /** Opens the builder to add a fresh session on [date]. */
    fun goBuild(date: LocalDate) {
        buildDate = date
        editingSession = null
        go(Screen.Build)
    }

    /** Opens the builder to edit [session] in place. */
    fun goEdit(session: PlannedSession) {
        buildDate = session.date
        editingSession = session
        go(Screen.Build)
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
        persist()
    }

    fun toggleRace() {
        raceOn = !raceOn
        persist()
    }

    /** Closes the kilometre in progress at whatever the clock says it took. */
    fun lap() {
        val lap = currentLapSeconds
        // A lap of nothing is a double tap, not a kilometre.
        if (lap <= 0) return
        runSplits.add(lap)
    }

    /** Clears the run board. The clock starts at zero and stopped. */
    private fun startRun() {
        runOn = false
        runSeconds = 0
        runSplits.clear()
    }

    /**
     * Closes the leg being raced and moves on. There is nothing to close on a
     * sim that has not started, and nothing to move on to once every leg is in.
     */
    fun nextLeg() {
        if (raceFinished) return
        val elapsed = currentLegSeconds
        if (elapsed <= 0) return
        legSeconds.add(elapsed)
        if (raceFinished) raceOn = false
        persist()
    }

    /** Clears the sim board, on the same terms as the run's. */
    private fun startRace() {
        raceOn = false
        raceSeconds = 0
        legSeconds.clear()
    }

    /** One second of wall clock: rest counts down, the timers count up. */
    fun tick() {
        if (restSeconds > 0) restSeconds--
        if (sessionLive) {
            sessionSeconds++
            // Often enough that a killed process loses seconds, not the session.
            if (sessionSeconds % 10 == 0) persist()
        }
        if (runOn) {
            runSeconds++
            if (runSeconds % 10 == 0) persist()
        }
        if (raceOn) {
            raceSeconds++
            if (raceSeconds % 10 == 0) persist()
        }
    }

    /** Writes the tracker's state against the active session. A banked session has no need to save. */
    private fun persist() {
        val session = activeSession?.takeIf { !it.done } ?: return
        val id = session.id
        // A run or a race saves its own clock; both keep the tracker's exercise
        // selection alongside, though only the strength session reads it back.
        val progress = when (session.kind) {
            SessionKind.RUN -> WorkoutProgress(runSeconds, 0, setsDone.toList())
            SessionKind.RACE -> WorkoutProgress(raceSeconds, legIndex, setsDone.toList())
            else -> WorkoutProgress(sessionSeconds, currentExercise, setsDone.toList())
        }
        viewModelScope.launch { workouts.save(id, progress) }
    }

    private fun restore(id: String, exercises: List<Exercise>) {
        viewModelScope.launch {
            val saved = workouts.load(id) ?: return@launch
            // The read is async: skip it if another session was opened meanwhile, or
            // if the session has been edited since and no longer lines up.
            if (activeSession?.id != id || saved.setsDone.size != exercises.size) return@launch
            when (activeSession?.kind) {
                SessionKind.RUN -> {
                    runSeconds = saved.elapsed
                    return@launch
                }
                SessionKind.RACE -> {
                    raceSeconds = saved.elapsed
                    return@launch
                }
                else -> Unit
            }
            saved.setsDone.forEachIndexed { index, done ->
                setsDone[index] = done.coerceIn(0, exercises[index].sets.size)
            }
            currentExercise = saved.currentExercise.coerceIn(0, exercises.lastIndex)
            sessionSeconds = saved.elapsed
        }
    }
}
