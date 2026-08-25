package com.repsrox.app.ui

/** The design's nine screens, plus the builder a real plan needs, each with the title it puts in the top bar. */
enum class Screen(val title: String) {
    Today("TODAY"),
    Plan("THE WEEK"),
    Plans("MY PLANS"),
    Build("NEW SESSION"),
    Live("SESSION"),
    Run("RUN"),
    Race("RACE SIM"),
    Fuel("FUEL"),
    Body("BODY"),
    Summary("SUMMARY"),
    Profile("PROFILE"),
}

/**
 * The bottom bar carries five destinations but ten screens hang off them, so
 * each tab owns a group and lights up for any screen inside it.
 */
enum class NavTab(val label: String, val root: Screen, val group: Set<Screen>) {
    Today("Today", Screen.Today, setOf(Screen.Today)),
    Train(
        "Train",
        Screen.Plan,
        setOf(Screen.Plan, Screen.Plans, Screen.Build, Screen.Live, Screen.Run, Screen.Summary),
    ),
    Race("Race", Screen.Race, setOf(Screen.Race)),
    Fuel("Fuel", Screen.Fuel, setOf(Screen.Fuel)),
    You("You", Screen.Profile, setOf(Screen.Profile, Screen.Body)),
}

/** Where Back goes: out to the tab's root, then to Today. */
fun Screen.parent(): Screen? = when (this) {
    Screen.Today -> null
    Screen.Plans, Screen.Build, Screen.Live, Screen.Run, Screen.Summary -> Screen.Plan
    Screen.Body -> Screen.Profile
    else -> Screen.Today
}
