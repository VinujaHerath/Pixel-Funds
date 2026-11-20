# Pixel Funds

A personal budget and expense tracking Android app built with **Kotlin**, **Jetpack Compose**, and **Material 3 Expressive** design principles — running **100% locally and privately** on your device with zero accounts or cloud dependencies.

---

## Highlights & Features

### 1. Calm, Scannable Home Dashboard
- **Remaining to Spend Hero Card**: Animated balance readout with a cycle countdown pill (`Xd left`, tap to inspect cycle progress or change your reset day), spend progress bar, and percentage used.
- **Glance Strip**:
  - **Safe / Day**: Live daily spending allowance and pace status (*On pace* vs. *Spending fast*), with an optional setting to reserve unpaid recurring bills due later in the cycle.
  - **Next Bill**: Shows your next upcoming recurring bill or subscription (with due countdown / overdue badges) and jumps directly to **Bills & Subs**.
- **6-Month Spending Trend**: Smooth cubic line chart powered by **Vico** (`compose-m3`), highlighting your monthly average and how the current cycle compares against prior months.
- **Top Categories**: Quick view of your top 3 spending categories this cycle; tap any category to open an expanded detail overlay with an animated wavy progress indicator and recent transactions.

### 2. Frictionless Expense Entry
- **Amount-First Numpad Sheet**: A prominent `+` FAB opens a tactile spring-animated numpad sheet with live animated currency formatting.
- **One-Tap Classification**: Shape-morphing category chips, optional merchant/note input, and a Material 3 `DatePicker` dialog.
- **Instant Recurring Scheduling**: Switch seamlessly between **One-time**, **Recurring**, and **Subscription** directly inside the entry sheet (with **Weekly**, **Every 2 wks**, **Monthly**, and **Yearly** cadences) to log an expense and track future bills in one step.

### 3. Budgets: Monthly Overview & Category Limits
- **Monthly Budget Overview** *(Room SQL aggregations)*:
  - Browse the active cycle and up to **24 prior months** using inline period controls.
  - Compare live monthly spending against a **custom overall monthly budget cap** or the sum of your category limits.
  - View budget health (*On track*, *Approaching*, *Over limit*), elapsed pace vs. spend ratio, safe daily allowance, projected month-end spend, largest expense, per-category breakdowns, and recent cycle expenses.
- **Category Limits & Customization**:
  - Tune individual category limits with haptic-feedback sliders and wavy progress bars.
  - Create, edit, or delete categories with **17 Material Rounded icons** and **10 accent color swatches** (deleting a category safely reassigns its transactions and recurring items).

### 4. Activity Feed, Recurring Bills & Subscriptions
- **Transactions Feed**:
  - Grouped-by-date list with daily totals, instant search across merchants, notes, and categories, and category filter chips.
  - **Swipe right** to edit or **swipe left** to delete with an instant **Undo** Snackbar.
- **Bills & Subscriptions Manager**:
  - Side-by-side summary cards comparing monthly **Recurring Bills** vs. **Subscriptions** plus combined monthly and yearly fixed commitments.
  - Filter by *Both*, *Recurring*, or *Subscriptions*, pause/resume items with a switch, edit schedules, or tap **Log Payment** to record an expense and advance the next due date automatically.

### 5. Rich Insights & Period Comparisons
- **Trends**: Interactive touch-and-drag scrubbable daily bar chart across **7D**, **14D**, **30D**, **90D**, and **Cycle** windows with a daily average reference line, peak-day callout, and a **Week-over-Week Velocity** card.
- **Monthly**: **6M / 12M** history bar chart plus head-to-head **Month A vs. Month B** comparison with quick presets (*vs Last Mo*, *vs 2 Mo Ago*, *vs 3 Mo Ago*, *vs Last Year*), a cumulative daily spend dual-line chart, and per-category deltas.
- **Yearly**: Multi-year totals snapshot, **Full Year vs. Year-to-Date (YTD)** toggle, a 12-month grouped bar chart comparing any two years, and category year-over-year deltas.
- **Mix**: Proportional category spending bar and breakdown scoped across **Cycle**, **Month**, **Year**, or **All** time.

### 6. Smart Category Budget Notifications
- **Automatic 80% & 100% Alerts**: Notifies you when spending in any category reaches **80%** or **100%+** of its monthly limit.
- **Smart Deduplication & Deep Linking**: Fires at most once per `(cycle, category, threshold)` unless spending drops back below the threshold, and tapping a notification opens the app directly onto that category's detail overlay.
- **Customizable Warning Threshold**: Tune the visual warning threshold (**55% – 95%**) in Settings, with Android 13+ (`POST_NOTIFICATIONS`) permission handling built in.

### 7. Onboarding, Guided Tour & Full Personalization
- **First-Launch Onboarding & 6-Step Interactive Tour**: 4-page illustrated welcome & quick setup flow (currency, reset day, starter sample data toggle) followed by an optional 6-step guided tour synced across live tabs. Replay either anytime from Home (`?`) or Settings.
- **All World Currencies**: Supports every circulating **ISO 4217** currency searchable by code, name, symbol, or country/region, plus a **Show Decimals** toggle.
- **Flexible Budget Cycle**: Choose any **Budget Reset Day** from the **1st to 31st** (automatically clamped for shorter months).
- **100% Local Backup & CSV Import/Export**:
  - **Export / Restore Full Backup (JSON)** covering preferences, categories, transactions, recurring items, and custom monthly limits.
  - **Export / Import CSV** with smart header detection and **Merge** or **Replace** options via Android's Storage Access Framework (no storage permissions required).
  - Starter sample data management and a confirmed **Clear All Transactions** option.
- **Appearance & Accessibility**:
  - **Light**, **Dark**, or **Material You Adaptive** themes (with optional Dynamic Color in manual Light/Dark themes on Android 12+).
  - **Compact Cards** density mode, **Haptic Feedback** toggle, and **Reduce Motion** support.

### 8. Adaptive Layout & Pixel Display Polish
- **Responsive Navigation**: Floating Pill Toolbar + vibrant `+` FAB on phones (`< 720dp`), switching automatically to a Material 3 `NavigationRail` on tablets, foldables, and wide displays (`>= 720dp`).
- **Expressive Motion & 120Hz Polish**: Distinct spring micro-interactions for every nav icon (**Home**, **Budgets**, **Activity**, **Insights**), draw-phase wavy progress indicators, custom **Outfit** + **Plus Jakarta Sans** typography with tabular numerals (`tnum`), edge-to-edge system bars, predictive back gestures, and Android 15+ LTPO `frameRateBoostOnTouchEnabled`.

---

## Tech Stack

- **Language & UI**: Kotlin (`2.4.21`), Jetpack Compose (BOM `2024.09.00`), Material 3 (Dynamic Color / Material You)
- **Architecture**: MVVM + Unidirectional Data Flow (`StateFlow` / `collectAsStateWithLifecycle`)
- **Local Persistence**: AndroidX Room (`2.7.0` + KSP) and Jetpack Preferences DataStore (`1.1.7`)
- **Data Visualization**: [Vico](https://github.com/patrykandpatrick/vico) (`com.patrykandpatrick.vico:compose-m3` `2.0.1`) + custom Compose `Canvas` charts
- **Testing**: JUnit 4, Robolectric (`4.16.1`), Jetpack Compose UI Test

---

## Project Structure

```text
app/src/main/java/com/example/
├── MainActivity.kt                        # Edge-to-edge host, theme wiring, deep links & 120Hz polish
├── data/
│   ├── AppPreferences.kt                  # DataStore preferences & ISO 4217 world currency catalog
│   ├── BudgetRepository.kt                # Repository abstraction
│   ├── CadenceModels.kt                   # Domain models, cycle date math & 6-month trend helpers
│   └── local/
│       ├── PixelFundsLocalDatabase.kt     # Room entities, DAOs, migrations & local repository
│       ├── MonthlyBudgetOverviewRepository.kt # Period-scoped monthly overview snapshot repository
│       └── LocalBackupCodec.kt            # Zero-dependency JSON full backup & RFC-4180 CSV codec
├── notifications/
│   └── BudgetAlertNotifier.kt             # 80% & 100% threshold evaluator & Android notifier
└── ui/
    ├── CadenceApp.kt                      # Adaptive root shell (Floating Pill Toolbar / NavigationRail)
    ├── CadenceViewModel.kt                # Workspace, CRUD, backup/import, alerts & tutorial state
    ├── MonthlyBudgetOverviewViewModel.kt  # Monthly budget comparison state & Room flows
    ├── theme/
    │   ├── Color.kt                       # Editorial Sage/Clay M3 palettes & financial status tokens
    │   ├── Theme.kt                       # Dynamic Color & preference CompositionLocals
    │   └── Type.kt                        # Outfit & Plus Jakarta Sans typography with tabular numerals
    ├── components/
    │   ├── ExpressiveComponents.kt        # Animated nav icons, floating pill toolbar & wavy progress
    │   └── DashboardSpendingTrendChart.kt # Vico 6-month spending trend card
    └── screens/
        ├── OnboardingScreen.kt            # 4-page welcome & quick setup flow
        ├── AppTutorialOverlay.kt          # Interactive 6-step guided tour
        ├── DashboardScreen.kt             # Home snapshot (Hero card, Safe/Day, Next Bill, Trend, Top 3)
        ├── BudgetsScreen.kt               # Overview / Category limits switcher & category editor sheet
        ├── MonthlyBudgetOverviewScreen.kt # Room-backed monthly spending vs. limit overview
        ├── ExpenseEntrySheet.kt           # Amount-first numpad sheet + inline recurring scheduling
        ├── TransactionsScreen.kt          # Searchable, swipeable activity feed
        ├── RecurringScreens.kt            # Bills & Subscriptions manager + editor sheet
        ├── InsightsScreen.kt              # Daily Trends interactive chart & weekly velocity
        ├── InsightsComparisons.kt         # Monthly, Yearly (Full/YTD) & Mix breakdown analytics
        └── SettingsBottomSheet.kt         # Full-screen Settings, themes, currencies & backup/import
```

---

## Building & Testing

Run the Gradle tasks from the project root:

```bash
# Build debug APK
gradle assembleDebug

# Run unit and Robolectric Compose UI tests
gradle :app:testDebugUnitTest
```

### Local Android Studio Setup

If you clone or download this repository outside AI Studio and `:app:validateSigningDebug` reports a missing `debug.keystore` in the project root, copy your default Android debug keystore into the project root:

```bash
# macOS / Linux
cp ~/.android/debug.keystore ./debug.keystore

# Windows (PowerShell)
Copy-Item "$env:USERPROFILE\.android\debug.keystore" ".\debug.keystore"
```

---

## Requirements

- **Min SDK**: 24 (Android 7.0)
- **Target / Compile SDK**: 36
- **JDK**: 11+
