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
- Updated `ExampleRobolectricTest` to expect `"Pixel Funds"` for `R.string.app_name`.

---

## [1.0.0] - Initial Release

### Added
- **Dashboard:** Remaining monthly budget hero squircle, live spend-pace indicator with wavy progress bar, safe daily spend allowance, and expandable top-category bento tiles.
- **Budgets:** Monthly category limits with slider tuning, custom category creation/editing, 17 Material icons, and 10 curated accent colors.
- **Activity:** Day-grouped transaction history with instant search, category filter chips, and swipe-to-delete with undo snackbar.
- **Expense Entry:** Tactile numpad keypad bottom sheet with quick Today/Yesterday chips and full calendar picker.
- **Insights:** Interactive tap-and-drag scrub bar chart (7D / 14D / 30D) with daily average reference line and proportional category mix breakdown.
- **Settings & Preferences:** DataStore-backed Light / Dark / Device adaptive theming, Material You dynamic color support, custom monthly budget reset day (1st–31st), and searchable catalog of 150+ ISO 4217 world currencies.
- **Local-First & Cloud Sync:** Instant offline persistence via Room (`PixelFundsLocalDatabase`) plus optional Google Sign-In and Firestore cloud sync with automatic local-to-cloud migration.
