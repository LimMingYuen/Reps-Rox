package com.repsrox.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import com.repsrox.app.data.EXERCISES
import com.repsrox.app.data.LEGS
import com.repsrox.app.data.MEALS
import com.repsrox.app.data.RUN_SECONDS_PER_KM
import java.util.Locale

/**
 * Everything the design's script block kept in component state. Initial values
 * are the design's, so the app opens mid-week with a session part-logged.
 */
class RepsRoxViewModel : ViewModel() {

    var screen by mutableStateOf(Screen.Today)
        private set

    /** Sets banked per exercise, indexed alongside [EXERCISES]. */
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
