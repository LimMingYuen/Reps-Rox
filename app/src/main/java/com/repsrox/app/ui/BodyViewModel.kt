package com.repsrox.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repsrox.app.data.WeighIn
import com.repsrox.app.data.WeightRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The Body screen's state. Kept apart from [RepsRoxViewModel] because weigh-ins
 * are the one thing in the app that outlive the process — everything else there
 * is still the design's fixed sample content.
 */
class BodyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeightRepository(application)

    /** Null until the first read off disk lands, so the screen doesn't flash its empty state. */
    val weighIns: StateFlow<List<WeighIn>?> = repository.weighIns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun log(date: LocalDate, kg: Float) {
        viewModelScope.launch { repository.add(WeighIn(date, kg)) }
    }

    fun delete(date: LocalDate) {
        viewModelScope.launch { repository.remove(date) }
    }
}
