# Changelog

All notable changes to **Pixel Funds** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- Repository documentation (`README.md`, `CHANGELOG.md`, `docs/ARCHITECTURE.md`, `docs/RELEASING.md`).
- GitHub Actions CI workflow (`.github/workflows/android-ci.yml`) running unit tests and building debug APK artifacts.
- Automated APK Release workflow (`.github/workflows/release-apk.yml`) publishing signed/debug APKs and SHA-256 checksums to GitHub Releases.
- Issue templates, pull request template, and Dependabot configuration.

### Fixed
- Fixed immediate startup crash when `google-services.json` is absent by guarding `FirebaseAuth` initialization in `MainActivity.kt`, `AuthScreen.kt`, and `FirestoreErrorHandler.kt`, plus wrapping display refresh setup safely.
- Resolved `default_web_client_id` dynamically in `AuthScreen.kt` so builds succeed when `google-services.json` is not present.
- Updated `ExampleRobolectricTest` to assert `"Pixel Funds"` for `R.string.app_name` and verify `MainActivity` launches cleanly in local mode without Firebase config.

---

## [1.0.0] - Initial Release

### Added
- **Dashboard:** Remaining monthly budget hero squircle, live spend-pace indicator with wavy progress bar, safe daily spend allowance (with optional upcoming bill reservation), fixed commitments card, and expandable top-category bento tiles.
- **Recurring Bills & Subscriptions:** Dedicated tracking for recurring bills and subscriptions (`weekly`, `biweekly`, `monthly`, `yearly`) with due-date countdowns, active/paused toggles, one-tap **Log paid**, and Firestore/Room persistence.
- **Budgets:** Monthly category limits with slider tuning, configurable warning thresholds (`55%–95%`), custom category creation/editing, 17 Material icons, and 10 curated accent colors.
- **Activity & CSV Export:** Day-grouped transaction history with instant search, category filter chips, swipe-to-delete with undo snackbar, and CSV export.
- **Expense Entry:** Tactile numpad keypad bottom sheet supporting One-time, Recurring, or Subscription logging with quick Today/Yesterday chips and full calendar picker.
- **4-Mode Insights:**
  - **Trends:** Interactive tap-and-drag scrub bar chart (7D / 14D / 30D) with daily average reference line.
  - **Monthly:** 6M/12M monthly history, side-by-side **Compare months**, cumulative Day 1–31 spend pace chart, and category comparison.
  - **Yearly:** Multi-year totals and year-over-year category breakdown.
  - **Mix:** Proportional category mix breakdown, fixed vs. flexible budget split, weekday spending pattern, and largest expenses list.
- **Settings, Pixel Feel & Preferences:** High refresh rate display mode, predictive back gesture support, DataStore-backed Light / Dark / Device adaptive theming, Material You dynamic colors, compact cards, Pixel tactile haptics, reduce-motion mode, decimal display toggle, custom budget reset day (1st–31st), and searchable catalog of 150+ ISO 4217 world currencies.
- **Local-First & Cloud Sync:** Instant offline persistence via Room (`PixelFundsLocalDatabase`) plus optional Google Sign-In and Firestore cloud sync with automatic local-to-cloud migration.
