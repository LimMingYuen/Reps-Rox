# RepsRox

An Android app for tracking workouts and reps.

## Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable)
- JDK 17+
- Android SDK

### Setup

1. Clone the repository:
   ```
   git clone <repo-url>
   cd RepsRox
   ```
2. Open the project folder in Android Studio.
3. Android Studio will detect the missing `gradlew` wrapper binary and offer to
   generate it automatically — accept the prompt (this repo was hand-scaffolded
   without a local JDK/Gradle installed, so the wrapper jar isn't checked in).
4. Let Gradle sync and download dependencies.
5. Run the app on an emulator or physical device (see below for options).

## Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- Gradle version catalog (`gradle/libs.versions.toml`)
- Min SDK 26, Target/Compile SDK 35

## Design

The UI implements the `Reps and Rox App` Claude Design document. It is dark-only
and brand-coloured — a near-black ground, one hot-orange accent (`#FF3B14`) used
as a line and a mark rather than a fill, Oswald for figures and headings, Inter
for everything else. Those decisions live in `ui/theme/`; screens take colour and
type from there rather than hard-coding values.

Fonts are the Oswald and Inter **variable** files, bundled in `res/font/`. Each
`FontWeight` registered in `Type.kt` pins the `wght` axis to its own instance.

## Project Structure

```
RepsRox/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/repsrox/app/
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/DemoData.kt        # Sample content, stands in for a store
│       │   │   └── ui/
│       │   │       ├── RepsRoxApp.kt       # Top bar, screen switch, bottom nav
│       │   │       ├── RepsRoxViewModel.kt # Session/timer state + clock formatting
│       │   │       ├── Screen.kt           # The nine screens and their nav grouping
│       │   │       ├── components/         # Mark, Panel, SegmentRing, Meter, rows
│       │   │       ├── screens/            # One file per screen
│       │   │       └── theme/              # Colour, type, theme
│       │   ├── res/                        # fonts, strings, themes, icons
│       │   └── AndroidManifest.xml
│       ├── test/                           # JVM unit tests
│       └── androidTest/                    # Instrumented tests
├── gradle/
│   ├── libs.versions.toml                  # Dependency version catalog
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts    # Project-level build configuration
└── settings.gradle.kts # Gradle project settings
```

### Screens

`Today`, `The week`, `My plans` (saved weeks), `New session` (the session
builder), `Session` (live strength logging), `Run`, `Race sim`, `Fuel`, `Body`,
`Summary` and `Profile`. Five bottom-bar destinations own them in groups — Train
covers the week, the plan shelf, the builder, the live session, run and summary;
You covers profile and body — and Back walks out to the group's root.

### What is real

The plan and the weigh-in log are written to disk (Preferences DataStore, one
record per line — see `PlanRepository`, `TemplateRepository` and
`WeightRepository`).

Sessions are built, opened, worked through and marked off from the app; finishing
one in the live tracker marks it off the week. The week screen shows one week at
a time and steps between them.

**Reusable plans.** A week worth repeating is saved from the week screen, then
laid down from `My plans` over as many weeks as you choose. Applying writes real
dated sessions, so a week can be changed afterwards without touching the plan it
came from; sessions already finished are never overwritten.

The plan store opens on a first-run seed drawn from the design's own content —
laid onto the week of first launch, with the days already past reading as banked
— so a fresh install is not an empty app; the first write folds the seed onto
disk. The race sim, run tracker, fuel log and profile are still fixed sample
data, as are the session clock and the personal-record banner on the summary.

## Running on a Device/Emulator

- **Android Studio's built-in emulator (AVD)** — default option, works out of the box.
- **Genymotion** — faster boot, free personal tier, good for UI iteration.
- **Physical device** — via USB or wireless debugging; recommended for testing
  real touch targets and outdoor/gym screen legibility.

## License

TBD
