package com.repsrox.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import com.repsrox.app.data.EXERCISES
import com.repsrox.app.data.Exercise
import com.repsrox.app.data.LEGS
import com.repsrox.app.data.MEALS
import com.repsrox.app.data.PlannedSession
import com.repsrox.app.data.RUN_SECONDS_PER_KM
import com.repsrox.app.data.SessionKind
import com.repsrox.app.data.weekStart
import java.time.LocalDate
import java.util.Locale

/**
 * Everything the design's script block kept in component state. Initial values
 * are the design's, so the app opens mid-week with a session part-logged.
 */
class RepsRoxViewModel : ViewModel() {

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
     * tracked keeps whatever has been banked into it; a different one starts clean.
     */
    fun open(session: PlannedSession) {
        if (session.id != activeSession?.id) {
            activeSession = session
            val exercises = session.exercises.takeIf { it.isNotEmpty() } ?: EXERCISES
            setsDone.clear()
            setsDone.addAll(List(exercises.size) { 0 })
            currentExercise = 0
            restSeconds = 0
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
    }

    fun selectExercise(index: Int) {
        currentExercise = index
    }

    fun skipRest() {
        restSeconds = 0
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

    /** One second of wall clock: rest counts down, the two timers count up. */
    fun tick() {
        if (restSeconds > 0) restSeconds--
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
