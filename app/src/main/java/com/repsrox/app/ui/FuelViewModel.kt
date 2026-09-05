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

    /**
     * The day being looked at. Held here rather than in the screen because the
     * app throws its screens away on every switch, and a day you stepped to is
     * worth keeping across one.
     */
    var day by mutableStateOf(LocalDate.now())
        private set

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

    /** Lays an imported document's meal days down, leaving every other day alone. */
    fun applyImport(parsed: ParsedPlan) {
        if (parsed.meals.isEmpty()) return
        viewModelScope.launch { repository.applyDays(parsed.meals) }
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
