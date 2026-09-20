package com.repsrox.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repsrox.app.data.Meal
import com.repsrox.app.data.MealRepository
import com.repsrox.app.data.ParsedPlan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/**
 * The Fuel screen's state. Kept apart from [RepsRoxViewModel] for the same
 * reason [BodyViewModel] and [PlanViewModel] are: meals outlive the process,
 * and the live tracker's state does not.
 */
class FuelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MealRepository(application)

    /** Null until the first read off disk lands, so the screen doesn't flash its empty state. */
    val meals: StateFlow<List<Meal>?> = repository.meals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Whether a meal week is repeating — a document of meals alone still needs stopping. */
    val repeating: StateFlow<Boolean> = repository.repeating
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * The day being looked at. Held here rather than in the screen because the
     * app throws its screens away on every switch, and a day you stepped to is
     * worth keeping across one.
     */
    var day by mutableStateOf(LocalDate.now())
        private set

    /**
     * A day the meal week prints is worked out at read time and never stored, so
     * it has to be made permanent as it passes — otherwise the morning takes with
     * it every day that was planned and never touched. Done once per launch.
     */
    init {
        viewModelScope.launch { repository.settle() }
    }

    fun goToDay(date: LocalDate) {
        day = date
    }

    /** Writes a meal, whether it is new or one already on the day being edited. */
    fun save(meal: Meal) {
        viewModelScope.launch { repository.save(meal) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.remove(id) }
    }

    fun toggleLogged(meal: Meal) {
        viewModelScope.launch { repository.setLogged(meal.id, !meal.logged) }
    }

    /**
     * Puts an imported document's meals in force. Its days are read as days of the
     * week, so it feeds every week from this one on — importing once is enough.
     */
    fun applyImport(parsed: ParsedPlan) {
        viewModelScope.launch { repository.applyImport(parsed.meals) }
    }

    /** Stops repeating the meal week, leaving only the days already written down. */
    fun clearRollingMeals() {
        viewModelScope.launch { repository.clearRolling() }
    }
}

/** A meal the dialog has just built, given the id it will be stored under. */
fun newMeal(
    date: LocalDate,
    name: String,
    detail: String,
    kcal: Int,
    proteinG: Int,
    carbsG: Int,
): Meal = Meal(
    id = UUID.randomUUID().toString(),
    date = date,
    name = name,
    detail = detail,
    kcal = kcal,
    proteinG = proteinG,
    carbsG = carbsG,
)
