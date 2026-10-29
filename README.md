# StrukKu

**Offline-first expense tracker for Android with on-device receipt scanning.**

Point the camera at a receipt and StrukKu reads the store name, total, date and category on the phone itself, with no internet and no account. You review the result, save it, and the home screen shows how much of this month's budget is left.

> The UI is in Bahasa Indonesia; code and documentation are in English.

## Screenshots

<!-- Add images to docs/screenshots/ and reference them here. -->
| Home | Scan | Confirm |
|---|---|---|
| _coming soon_ | _coming soon_ | _coming soon_ |

## Features

- **Receipt scanning, fully offline**: CameraX capture + ML Kit text recognition (bundled model), parsed with rules tuned for Indonesian receipts (`TOTAL`, `GRAND TOTAL`, `JUMLAH`, `25.000`, `25.000,00`, `48,000.00`, `29 Okt 2026`, …).
- **Review before saving**: scanned values pre-fill the form; anything the scan could not read is flagged "Periksa kembali".
- **Monthly budget**: remaining budget with a progress bar that turns amber at 80% and red when overspent.
- **Spending by category and recent transactions** on the home screen, updated live.
- **Statistics**: six-month spending bars and a category donut, both drawn by hand on a Compose `Canvas`, with text descriptions for screen readers.
- **Budget alerts**: a notification at 80% and when the budget is used up, at most once per level per month. Checked in the background with WorkManager after every save and once a day.
- **Manual entry** with a thousands-grouped amount field, a date picker that blocks future dates, and a category dropdown.
- **Private by design**: no `INTERNET` permission; photos are processed in memory and never written to storage.

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Navigation Compose (type-safe routes) · MVVM · Hilt · Room · Coroutines & Flow · CameraX · ML Kit Text Recognition · JUnit 4 · MockK · Turbine · Gradle Kotlin DSL with a version catalog

## Architecture

```mermaid
flowchart LR
    subgraph ui [ui]
        Screen[Composable screens] --> VM[ViewModels<br/>StateFlow&lt;UiState&gt;]
    end
    subgraph domain [domain: pure Kotlin]
        UC[Use cases] --> RepoI[Repository interfaces]
        UC --> Parser[ReceiptRowBuilder<br/>ReceiptParser<br/>CategoryClassifier]
    end
    subgraph data [data]
        RepoImpl[Room repositories] --> Room[(Room DB)]
        OCR[ML Kit recognizer]
    end
    VM --> UC
    VM --> OCR
    RepoImpl -. implements .-> RepoI
```

- **`domain` has no Android imports.** Models, use cases and the receipt parser are plain Kotlin, so the logic most likely to break is tested on the JVM in milliseconds.
- **One `UiState` per screen.** Composables are stateless (`XxxScreen`) and previewable; a thin `XxxRoute` connects them to the ViewModel.
- **Room Flows all the way up.** The home summary `combine`s four queries, so the screen updates by itself whenever data changes.

### Decisions worth a closer look

| Decision | Why |
|---|---|
| Money is `Long` rupiah | No floating-point rounding errors; rupiah has no minor unit in practice. |
| `ReceiptRowBuilder` | OCR returns receipt columns as separate blocks ("TOTAL" … "99.330"). Grouping fragments by vertical position rebuilds the printed rows, without which the total cannot be found reliably. |
| Rule-based parser instead of an ML model | Instant, transparent, and every rule is pinned by a test with a realistic receipt. Mistakes are fixed on the Confirm screen. |
| Whole-word keyword matching, store name weighted ×3 | "tol" (toll road) must not match "toleransi"; a pharmacy that sells coffee is still a pharmacy. |
| Manual budget upsert | SQLite treats `NULL`s as distinct in unique indexes, so `@Upsert` would allow two "overall" budgets per month. |
| Scanned draft as a navigation argument | Survives rotation and process death. Encoded as URL-safe Base64 JSON so user text like `Kopi & Roti 100%` can't be corrupted by URL decoding (caught by an instrumented test). |
| Charts on `Canvas`, geometry in `ChartGeometry` | No chart library; bar heights and donut angles are plain functions with unit tests, the composables only draw them. |
| Room AutoMigration with a migration test | Adding the alerts table bumped the schema to v2. Room generates the migration from the committed schema files, and `MigrationTest` upgrades a real v1 database to prove no user data is lost. |
| Alert logic in the domain, WorkManager at the edge | `CheckBudgetAlertUseCase` decides (and is unit tested); the worker just runs it. Every reached level is recorded, so jumping from 50% to 120% sends one "used up" alert, never a late "nearly used". |
| Injected `Clock` | "Today" and "this month" are testable, including crossing midnight at month end. |

## Project structure

```
app/src/main/java/com/hanan/strukku/
├─ data/      Room entities, DAOs, repositories, ML Kit recognizer, Hilt modules
├─ domain/    models, repository interfaces, use cases, receipt parser
└─ ui/        theme, navigation, components, home/, scan/, confirm/
```

## Build and test

Requires Android Studio (bundled JDK) and an Android 10+ device or emulator.

```bash
./gradlew assembleDebug                 # build the APK
./gradlew testDebugUnitTest             # JVM unit tests (parser, use cases, ViewModels)
./gradlew connectedDebugAndroidTest     # device tests (Room DAOs, navigation arguments)
./gradlew lintDebug
```

## Roadmap

Development happens in phases; see [SPEC.md](SPEC.md) for the full spec.

- [x] 1. Foundation: Hilt, navigation, theme
- [x] 2. Data and domain: Room, repositories, use cases
- [x] 3. Home screen and manual entry
- [x] 4. Receipt scanning
- [x] 5. Scan confirmation and MVP polish
- [ ] 6. Monthly charts ✅ · budget notifications ✅ · PDF export
