# Pixel Funds

A personal budget tracking Android app built with **Kotlin**, **Jetpack Compose**, and **Material 3 Expressive** design principles — running 100% locally and privately on your device.

## Highlights

- **Calm, Scannable Dashboard**:
  - Unified **Remaining to Spend** hero card with cycle countdown pill and budget progress bar.
  - **Safe / Day** pace indicator and **Next Bill** glance strip.
  - Interactive **6-Month Spending Trend** chart powered by **Vico** (`compose-m3`), showing your monthly average and how the current month compares.
  - Grouped **Top Categories** summary with shared-element category detail expansion.
- **Frictionless Expense Entry**:
  - Prominent morphing `+` FAB opens an amount-first numpad sheet with one-tap category chips, date selection, and optional **Recurring Bill** or **Subscription** scheduling.
- **Monthly Budget Overview, Category Limits & Smart Alerts**:
  - Compare live monthly spending against a custom overall monthly cap or your combined category limits, browse previous months, and tune individual category targets with haptic sliders.
  - Automatic **Category Budget Notifications** alert you when spending in any category reaches **80%** or exceeds **100%** of its monthly limit, with per-cycle deduplication and tap-to-inspect deep linking.
- **Activity, Recurring Bills & Subscriptions**:
  - Grouped-by-date transaction feed with instant search, category filters, and swipe-to-delete with Undo.
  - Dedicated **Bills & Subs** view keeping recurring bills and subscriptions side-by-side with one-tap payment logging.
- **Insights & Period Comparisons**:
  - Interactive daily spending charts, category mix breakdowns, month-over-month comparisons, and year-over-year trends.
- **First-Launch Onboarding & Interactive 6-Step Tutorial**:
  - Illustrated welcome slides with quick setup for currency, budget reset day, and starter sample data.
  - Replayable 6-step guided tour accessible anytime from the Home `?` button or Settings.
- **100% Local & Private with Full Backup / Import**:
  - No account or cloud required — all data stays on-device in **Room** and **DataStore**.
  - **Export full backup (JSON)** and **Import backup or CSV** using the Android Storage Access Framework (zero storage permissions required).
  - Full world currency support (all circulating ISO 4217 currencies), customizable monthly budget reset day, and **Light**, **Dark**, or **Material You Adaptive** theming.
- **Expressive Motion & Pixel Polish**:
  - Distinct spring-physics click animations for every bottom navigation icon (**Home**, **Budgets**, **Activity**, **Insights**), morphing pill shapes, tactile haptics, and full **Reduce Motion** accessibility support.

## Tech Stack

- **Language & UI**: Kotlin, Jetpack Compose, Material 3 (Dynamic Color / Material You)
- **Architecture**: MVVM + Unidirectional Data Flow (`StateFlow` / `collectAsStateWithLifecycle`)
- **Local Persistence**: AndroidX Room (SQLite + KSP) and Jetpack Preferences DataStore
- **Data Visualization**: [Vico](https://github.com/patrykandpatrick/vico) (`com.patrykandpatrick.vico:compose-m3`)
- **Testing**: JUnit 4, Robolectric, Jetpack Compose UI Test

## Project Structure

```text
app/src/main/java/com/example/
├── MainActivity.kt                      # Edge-to-edge host activity & Pixel display polish
├── data/
│   ├── AppPreferences.kt                # DataStore preferences & world currency catalog
│   ├── BudgetRepository.kt              # Repository abstraction
│   ├── CadenceModels.kt                 # Domain models, budget cycle math & 6-month trend helpers
│   └── local/
│       ├── PixelFundsLocalDatabase.kt   # Room database, DAOs, migrations & local repository
│       ├── MonthlyBudgetOverviewDao.kt  # Room SQL aggregations for monthly overview
│       └── LocalBackupCodec.kt          # Zero-dependency JSON full backup & RFC-4180 CSV codec
├── notifications/
│   └── BudgetAlertNotifier.kt           # 80% & 100% category threshold evaluator & Android notifier
└── ui/
    ├── CadenceApp.kt                    # Adaptive root shell (Floating Pill Toolbar / NavigationRail)
    ├── CadenceViewModel.kt              # Core workspace, onboarding & tutorial state management
    ├── MonthlyBudgetOverviewViewModel.kt# Monthly budget comparison state & Room flows
    ├── theme/                           # Material 3 Expressive typography, motion & tonal color tokens
    ├── components/
    │   ├── ExpressiveComponents.kt      # Animated bottom nav icons, floating toolbar & wavy indicators
    │   └── DashboardSpendingTrendChart.kt # Vico 6-month spending trend card
    └── screens/
        ├── OnboardingScreen.kt          # 4-page welcome & quick setup flow
        ├── AppTutorialOverlay.kt        # Interactive 6-step guided tour
        ├── DashboardScreen.kt           # Decluttered Home snapshot
        ├── MonthlyBudgetOverviewScreen.kt # Room-backed monthly spending vs limit overview
        ├── BudgetsScreen.kt             # Overview / Category limits switcher & editor
        ├── ExpenseEntrySheet.kt         # Fast numpad expense entry bottom sheet
        ├── TransactionsScreen.kt        # Searchable activity feed + Bills & Subs management
        ├── InsightsScreen.kt            # Multi-month & multi-year analytics
        └── SettingsBottomSheet.kt       # Themes, currencies, reset day, backup/import & guide replay
```

## Building & Testing

Run the Gradle tasks from the project root:

```bash
# Build debug APK
gradle assembleDebug

# Run unit and Robolectric Compose UI tests
gradle :app:testDebugUnitTest
```

## Requirements

- **Min SDK**: 24 (Android 7.0)
- **Target / Compile SDK**: 36
- **JDK**: 11+
