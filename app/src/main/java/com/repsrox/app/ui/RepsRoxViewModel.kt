package com.repsrox.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repsrox.app.data.EXERCISES
import com.repsrox.app.data.Exercise
import com.repsrox.app.data.LEGS
import com.repsrox.app.data.MEALS
import com.repsrox.app.data.PlannedSession
import com.repsrox.app.data.RUN_SECONDS_PER_KM
import com.repsrox.app.data.SessionKind
import com.repsrox.app.data.WorkoutProgress
import com.repsrox.app.data.WorkoutRepository
import com.repsrox.app.data.weekStart
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/**
 * Everything the design's script block kept in component state. Initial values
 * are the design's, so the app opens mid-week with a session part-logged.
 */
class RepsRoxViewModel(application: Application) : AndroidViewModel(application) {

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

    /** Sets banked per exercise, indexed alongside [activeExercises]. */
    val setsDone = mutableStateListOf(2, 0, 0, 0, 0)

    var currentExercise by mutableIntStateOf(0)
        private set

    var restSeconds by mutableIntStateOf(96)
        private set

    /** The session clock. It only runs once Start is pressed, never on arrival. */
    var liveSeconds by mutableIntStateOf(0)
        private set

    var liveOn by mutableStateOf(false)
        private set

    // Like the session clock, the run and race clocks wait for Start.
    var runSeconds by mutableIntStateOf(0)
        private set

    var runOn by mutableStateOf(false)
        private set

    var raceSeconds by mutableIntStateOf(0)
        private set

    var raceOn by mutableStateOf(false)
        private set

    var legIndex by mutableIntStateOf(0)
        private set

    /** Keys of the meals checked off today. */
    val mealsLogged = listOf("b", "l").toMutableStateList()

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

    /** How long the active session ran, off whichever clock timed it. */
    val sessionSeconds: Int
        get() = when (activeSession?.kind) {
            SessionKind.RUN -> runSeconds
            SessionKind.RACE -> raceSeconds
            else -> liveSeconds
        }

    val leg get() = LEGS[legIndex]

    /** Stations reached so far — the number the race ring and dial show. */
    val stationNumber: Int
        get() = LEGS.take(legIndex + 1).count { it.isStation }

    /** Fixed to [Locale.US] so the decimal separator matches the rest of the design. */
    val runKilometres: String
        get() = String.format(Locale.US, "%.2f", runSeconds / RUN_SECONDS_PER_KM)

    // ── Intents ─────────────────────────────────────────────────────────────

    fun go(destination: Screen) {
        screen = destination
    }

    fun goToWeek(start: LocalDate) {
        weekStart = start
    }

    /** Opens the builder against [date], so a session lands on the day it was added from. */
    fun goBuild(date: LocalDate) {
        buildDate = date
        screen = Screen.Build
    }

    /**
     * Opens a session off the plan: what has been done opens its summary, what is
     * still ahead opens the tracker it needs. Reopening the session already being
     * tracked keeps whatever has been banked into it; a different one picks up
     * whatever was saved against it, or starts clean. The clock always comes back
     * paused, so it never runs without being asked to.
     */
    fun open(session: PlannedSession) {
        if (session.id != activeSession?.id) {
            persist()
            activeSession = session
            val exercises = session.exercises.takeIf { it.isNotEmpty() } ?: EXERCISES
            setsDone.clear()
            setsDone.addAll(List(exercises.size) { 0 })
            currentExercise = 0
            restSeconds = 0
            liveSeconds = 0
            liveOn = false
            when (session.kind) {
                SessionKind.RUN -> {
                    runSeconds = 0
                    runOn = false
                }
                SessionKind.RACE -> {
                    raceSeconds = 0
                    raceOn = false
                    legIndex = 0
                }
                else -> Unit
            }
            if (!session.done) restore(session.id, exercises)
        }
        screen = when {
            session.done -> Screen.Summary
            session.kind == SessionKind.RUN -> Screen.Run
            session.kind == SessionKind.RACE -> Screen.Race
            else -> Screen.Live
        }
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

    fun toggleLive() {
        liveOn = !liveOn
        persist()
    }

    /** Stops the clock and drops the saved progress — a banked session has none to resume. */
    fun finishLive() {
        liveOn = false
        runOn = false
        activeSession?.let { session -> viewModelScope.launch { workouts.clear(session.id) } }
    }

    fun skipRest() {
        restSeconds = 0
    }

    fun toggleRun() {
        runOn = !runOn
        persist()
    }

    fun toggleRace() {
        raceOn = !raceOn
        persist()
    }

    fun nextLeg() {
        legIndex = (legIndex + 1).coerceAtMost(LEGS.lastIndex)
        persist()
    }

    fun toggleMeal(key: String) {
        if (!mealsLogged.remove(key)) mealsLogged.add(key)
    }

    fun isMealLogged(key: String) = key in mealsLogged

    /** One second of wall clock: rest counts down, the two timers count up. */
    fun tick() {
        if (restSeconds > 0) restSeconds--
        if (liveOn) {
            liveSeconds++
            // Often enough that a killed process loses seconds, not the session.
            if (liveSeconds % 10 == 0) persist()
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

    /** Writes the tracker's state against the active session. Demo content has no id to save under. */
    private fun persist() {
        val session = activeSession?.takeIf { !it.done } ?: return
        val id = session.id
        // A run or a race saves its own clock; a race keeps its leg where the
        // tracker keeps its exercise.
        val progress = when (session.kind) {
            SessionKind.RUN -> WorkoutProgress(runSeconds, 0, setsDone.toList())
            SessionKind.RACE -> WorkoutProgress(raceSeconds, legIndex, setsDone.toList())
            else -> WorkoutProgress(liveSeconds, currentExercise, setsDone.toList())
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
                    legIndex = saved.currentExercise.coerceIn(0, LEGS.lastIndex)
                    return@launch
                }
                else -> Unit
            }
            saved.setsDone.forEachIndexed { index, done ->
                setsDone[index] = done.coerceIn(0, exercises[index].sets.size)
            }
            currentExercise = saved.currentExercise.coerceIn(0, exercises.lastIndex)
            liveSeconds = saved.elapsed
        }
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
