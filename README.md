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

## Project Structure

```
RepsRox/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/repsrox/app/       # MainActivity, Compose UI, theme
│       │   ├── res/                        # strings, themes, icons
│       │   └── AndroidManifest.xml
│       ├── test/                           # JVM unit tests
│       └── androidTest/                    # Instrumented tests
├── gradle/
│   ├── libs.versions.toml                  # Dependency version catalog
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts    # Project-level build configuration
└── settings.gradle.kts # Gradle project settings
```

## Running on a Device/Emulator

- **Android Studio's built-in emulator (AVD)** — default option, works out of the box.
- **Genymotion** — faster boot, free personal tier, good for UI iteration.
- **Physical device** — via USB or wireless debugging; recommended for testing
  real touch targets and outdoor/gym screen legibility.

## License

TBD
