# Architecture Overview

Pixel Funds is built with **Kotlin**, **Jetpack Compose**, and a reactive **MVVM + Repository** architecture that supports instant on-device persistence and seamless optional cloud synchronization.

---

## High-Level Layers

```text
MainActivity (Edge-to-edge + Theme + Auth session switch)
   └── CadenceApp (SharedTransitionLayout + FloatingPillToolbar / NavigationRail)
         ├── DashboardScreen     (Remaining squircle, spend pace, top categories)
         ├── BudgetsScreen       (Monthly limit sliders & category editor sheet)
         ├── TransactionsScreen  (Search, category filter, swipe-to-delete + undo)
         ├── InsightsScreen      (Interactive 7D/14D/30D scrub chart & category mix)
         ├── ExpenseEntrySheet   (Custom numpad & date picker)
         └── SettingsBottomSheet (Appearance, reset day, world currencies, Google sync)
               │
               ▼
         CadenceViewModel (StateFlow<CadenceUiState> + one-shot events)
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
- Existing on-device categories and transactions are migrated via `localMigrationSource` so no local data is lost.

### 2. Timezone-Safe Civil Calendar (`LocalDay`)
- `LocalDay` implements Howard Hinnant's civil-date epoch math (`toEpochDay()` / `fromEpochDay()`) compatible with **API 24+**.
- Custom monthly reset cycles (`budgetResetDay` from 1st to 31st) clamp cleanly for shorter months (e.g., Feb 28/29).

### 3. Material 3 Expressive Design System
- **Adaptive & Manual Themes:** `AppThemeMode` supports `ADAPTIVE_DEVICE` (Material You dynamic color on Android 12+), `LIGHT`, and `DARK`, with an optional dynamic-color toggle in manual modes.
- **Fallback Palette:** Forest Sage (`#1E6444`), Warm Clay (`#4E6356`), and Sand (`#835423`).
- **Typography:** `Outfit` for display/headings and `Plus Jakarta Sans` for UI body and labels.
- **Physics-based Motion:** `ExpressiveMotion` defines spatial overshoot springs for layout transitions and critically damped springs for color/alpha changes.

### 4. Global World Currency Support
- `WorldCurrencyCatalog` dynamically indexes circulating ISO 4217 currencies from the Android/JVM ICU dataset with preferred compact symbols and country/region search metadata.

### 5. Zero-Trust Firestore Security
- Data is isolated under `/users/{userId}`, `/users/{userId}/categories/{categoryId}`, and `/users/{userId}/transactions/{transactionId}`.
- `firestore.rules` enforces strict ownership, field type/range validation, and server timestamp integrity, verified by `firestore.test.js` and documented in `firebase-blueprint.json`.
