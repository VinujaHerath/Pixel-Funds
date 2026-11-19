# Architecture Overview

Pixel Funds is built with **Kotlin**, **Jetpack Compose**, and a reactive **MVVM + Repository** architecture that supports instant on-device persistence and seamless optional cloud synchronization.

---

## High-Level Layers

```text
MainActivity (Edge-to-edge + Smooth Display + Theme + Auth session switch)
   └── CadenceApp (SharedTransitionLayout + FloatingPillToolbar / NavigationRail)
         ├── DashboardScreen               (Remaining squircle, spend pace, fixed commitments, top categories)
         ├── BudgetsScreen                 (Monthly limit sliders & category editor sheet)
         ├── TransactionsScreen            (Transactions log + Recurring & Subscriptions switcher)
         │     └── RecurringScreens        (Bills/subs summary, due countdowns, Log paid, editor sheet)
         ├── InsightsScreen                (4-mode switcher: Trends | Monthly | Yearly | Mix)
         │     └── InsightsComparisons     (6M/12M history, month-over-month, YoY, fixed vs. flexible)
         ├── ExpenseEntrySheet             (Custom numpad, one-time / recurring / sub creation, date picker)
         └── SettingsOverlayScreen         (Appearance, Pixel feel, reset day, currencies, CSV export, sync)
               │
               ▼
         CadenceViewModel (StateFlow<CadenceUiState> + one-shot events + CSV export)
               │
               ├── BudgetRepository (interface)
               │     ├── LocalBudgetRepository (Room: PixelFundsLocalDatabase)
               │     └── CadenceRepository     (Cloud Firestore + local migration)
               │
               └── AppPreferencesRepository (Jetpack DataStore Preferences)
```

---

## Key Design Decisions

### 1. Local-First with Seamless Cloud Upgrade
- Users land immediately in **On-device mode** backed by Room (`PixelFundsLocalDatabase`) — no sign-in wall.
- When a user signs in with Google (via Android **Credential Manager** + **Firebase Auth**), `MainActivity` swaps the session key to `cloud_<uid>` and instantiates `CadenceRepository`.
- Existing on-device categories, transactions, and recurring schedules are migrated via `localMigrationSource` so no local data is lost.

### 2. Recurring Commitments & Smart Safe-Daily Math
- `RecurringItem` tracks both bills (`RecurringKind.RECURRING`) and subscriptions (`RecurringKind.SUBSCRIPTION`) across `weekly`, `biweekly`, `monthly`, and `yearly` `BillingCadence` intervals.
- Users can toggle `includeFixedBillsInSafeDaily` in preferences to reserve upcoming active commitments from their `Safe / day` budget calculation, and mark schedules paid in one tap (`logRecurringPayment`).

### 3. Timezone-Safe Civil Calendar (`LocalDay`)
- `LocalDay` implements Howard Hinnant's civil-date epoch math (`toEpochDay()` / `fromEpochDay()`) compatible with **API 24+**.
- Custom monthly reset cycles (`budgetResetDay` from 1st to 31st) clamp cleanly for shorter months (e.g., Feb 28/29).

### 4. Pixel-Tuned Display & Material 3 Expressive Design System
- **Smooth Display & Predictive Back:** `MainActivity.enablePixelSmoothDisplay()` selects the highest supported refresh rate at the display's native resolution on Android 11+, and `AndroidManifest.xml` opts into `android:enableOnBackInvokedCallback="true"`.
- **Adaptive & Manual Themes:** `AppThemeMode` supports `ADAPTIVE_DEVICE` (Material You dynamic color on Android 12+), `LIGHT`, and `DARK`, with an optional dynamic-color toggle in manual modes.
- **Fallback Palette:** Forest Sage (`#1E6444`), Warm Clay (`#4E6356`), and Sand (`#835423`).
- **Typography:** `Outfit` for display/headings and `Plus Jakarta Sans` for UI body and labels.
- **Motion & Accessibility:** `ExpressiveMotion` defines spatial overshoot springs and critically damped effect springs, paired with `reduceMotion`, `hapticsEnabled`, `compactCards`, and `showDecimals` user preferences.

### 5. Global World Currency Support & Data Portability
- `WorldCurrencyCatalog` dynamically indexes circulating ISO 4217 currencies from the Android/JVM ICU dataset with preferred compact symbols and country/region search metadata.
- Users can export their full transaction history to a **CSV** file directly from Settings.

### 6. Zero-Trust Firestore Security
- Data is isolated under `/users/{userId}`, `/users/{userId}/categories/{categoryId}`, `/users/{userId}/transactions/{transactionId}`, and `/users/{userId}/recurring/{recurringId}`.
- `firestore.rules` enforces strict ownership, field type/range/enum validation, and server timestamp integrity, verified by `firestore.test.js` and documented in `firebase-blueprint.json`.
