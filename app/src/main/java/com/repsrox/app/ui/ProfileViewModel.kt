package com.repsrox.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.repsrox.app.data.PersonalRecord
import com.repsrox.app.data.ProfileRepository
import com.repsrox.app.data.Race
import com.repsrox.app.data.RaceRepository
import com.repsrox.app.data.SessionRepository
import com.repsrox.app.data.WeightRepository
import com.repsrox.app.data.bestLifts
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the Profile screen reads off disk, in one shape. Held together
 * rather than as four flows so the screen can wait for all of it — a profile
 * that says "no race booked" for a frame over a race that is booked reads worse
 * than one that opens a frame late.
 */
data class ProfileState(
    /** The name set for this install, or null while nobody has set one. */
    val name: String?,
    /** The latest weigh-in, or null while nothing has been logged. */
    val latestKg: Float?,
    val race: Race?,
    val lifts: List<PersonalRecord>,
)

/**
 * The Profile screen's state. Beyond the name it owns no store of its own: the
 * weight is the log the Body screen keeps and the lifts are the sessions the
 * live tracker banks, so the profile can only ever agree with the screens they
 * came from.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val profile = ProfileRepository(application)
    private val weights = WeightRepository(application)
    private val sessions = SessionRepository(application)
    private val races = RaceRepository(application)

    /** Null until the first read off disk lands, so the screen doesn't flash its empty state. */
    val state: StateFlow<ProfileState?> =
        combine(
            profile.name,
            weights.weighIns,
            sessions.sessions,
            races.race,
        ) { name, weighIns, log, race ->
            ProfileState(
                name = name,
                latestKg = weighIns.lastOrNull()?.kg,
                race = race,
                lifts = bestLifts(log),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Names the install, or clears the name when what was typed is blank. */
    fun rename(name: String) {
        viewModelScope.launch { profile.setName(name) }
    }

    /** Books a race, replacing whatever was booked before it. */
    fun book(race: Race) {
        viewModelScope.launch { races.book(race) }
    }

    fun clearRace() {
        viewModelScope.launch { races.clear() }
    }
}
