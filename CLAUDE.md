# CLAUDE.md

Guidance for Claude Code when working in this repository.

## About the app
**MySavings** is an offline-first Android expense tracker with on-device receipt scanning.
The user photographs a receipt, ML Kit reads the text on the device (no internet), and the
app pre-fills total, date, merchant and category. The user corrects the result and saves it.
All data is stored locally. This is a portfolio project, so code clarity matters as much as features.

Product spec, data model and phase plan: see [SPEC.md](SPEC.md).

## Stack
- Kotlin (built into AGP 9; no separate `kotlin-android` plugin), Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`)
- Jetpack Compose with Material 3, Navigation Compose (type-safe routes via `@Serializable`)
- MVVM with `ui` / `domain` / `data` layers
- Hilt for DI, KSP for annotation processing (no kapt)
- Room for local storage; Coroutines + Flow
- WorkManager (background budget checks)
- CameraX + ML Kit Text Recognition, **bundled** model (`com.google.mlkit:text-recognition`) so OCR works offline from first install
- Tests: JUnit 4, MockK, kotlinx-coroutines-test, Turbine

minSdk 29, targetSdk/compileSdk 37, JVM target 11.

## Package structure
Single `:app` module, package by layer, then by feature:

```
com.hanan.mysavings
├─ data/
│  ├─ local/        Room entities, DAOs, MySavingsDatabase, default category seed
│  ├─ repository/   repository implementations + entity <-> domain mappers
│  ├─ ocr/          ML Kit text recognizer wrapper
│  └─ di/           Hilt modules
├─ notification/  budget alerts: WorkManager worker, scheduler, notifier (Android plumbing for a domain use case)
├─ export/        PDF report: page layout (pure Kotlin) and PdfDocument writer
├─ domain/
│  ├─ model/        plain Kotlin data classes
│  ├─ repository/   repository interfaces
│  ├─ usecase/      one action per class
│  └─ parser/       ReceiptParser, CategoryClassifier
└─ ui/
   ├─ theme/  navigation/  components/  util/
   └─ home/  scan/  confirm/  stats/   (one package per screen)
```

Dependency rule: `ui` → `domain` ← `data`. `domain` must not import Android or `data` classes.

## Code conventions
- **Language**: all user-facing text in Bahasa Indonesia, defined in `res/values/strings.xml` (never hardcoded in composables). Code, identifiers, comments and commit messages in English.
- **Money** is `Long` in whole rupiah. Never use `Double`/`Float` for amounts. Format with a shared `formatRupiah()` helper (e.g. `Rp25.000`).
- **Dates** use `java.time` (`LocalDate`, `YearMonth`). Entities store plain numbers (`LocalDate` as epoch day, `YearMonth` as `yyyyMM` Int, `Instant` as epoch millis), and conversion happens in `data/repository/Mappers.kt`. No Room TypeConverters.
- **Time**: never call `LocalDate.now()` directly in logic; inject `java.time.Clock` so tests can fix "today".
- **Domain is pure Kotlin** so it can be unit tested on the JVM without an emulator.
- **Room entities stay in `data`**; map to domain models with `toDomain()` / `toEntity()` extension functions.
- **Screens** follow this pattern:
  - `XxxScreen(uiState, onEvent...)`: stateless and previewable
  - `XxxRoute()`: obtains the ViewModel via `hiltViewModel()`, collects state with `collectAsStateWithLifecycle()`
  - `XxxViewModel`: exposes `StateFlow<XxxUiState>`; one-off events (navigation, snackbar) via a separate channel/flow
- **Use cases**: one responsibility each, exposed as `operator fun invoke(...)`.
- **Dispatchers**: when code needs to switch threads, inject the dispatcher with a Hilt qualifier (`@IoDispatcher`) instead of hardcoding it, so tests can swap it. Room suspend/Flow queries already run off the main thread, so they need no dispatcher.
- **Dependencies** are always added through the version catalog, never as inline strings.
- **Tests**: every non-UI piece of logic (parser, classifier, use cases, ViewModels) gets unit tests in `src/test`. DAO tests go in `src/androidTest` with an in-memory database.
- Prefer small, readable functions and comments that explain *why*, not *what*.

## Commands (Windows, from project root)
| Task | Command |
|---|---|
| Build debug APK | `.\gradlew.bat assembleDebug` |
| Unit tests | `.\gradlew.bat testDebugUnitTest` |
| Lint | `.\gradlew.bat lintDebug` |
| Instrumented tests (device/emulator required) | `.\gradlew.bat connectedDebugAndroidTest` |

`JAVA_HOME` is not set globally on this machine. In a terminal session, use Android Studio's bundled JDK first:
`$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`

## Working agreement
- Work on **one phase (or step) per request**. Present a short plan first and wait for approval before changing code.
- After finishing, run `assembleDebug` and `testDebugUnitTest`, then summarize what changed and the test results (report failures honestly).
- End every phase with a commit (`feat: phase N ...`) and `git push` to `origin main`, so progress per phase is visible on GitHub.
- Briefly explain every architectural decision. The owner wants to understand the code.
- Don't add features or dependencies outside the approved plan.
