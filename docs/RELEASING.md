# Releasing Pixel Funds & Publishing APKs

Pixel Funds uses [GitHub Actions](.github/workflows/release-apk.yml) to automatically build, test, checksum, and publish installable APKs to **GitHub Releases**.

---

## Quick Release Checklist

1. **Bump the app version** in `app/build.gradle.kts`:
   ```kotlin
   defaultConfig {
     versionCode = 2        // Increment by 1 for every release
     versionName = "1.1.0"  // Match the git tag without the 'v' prefix
   }
   ```
2. **Update `CHANGELOG.md`** with the new version's changes.
3. **Commit and push** your changes to `main`.
4. **Create and push a version tag** (or run the workflow manually):
   ```bash
   git tag -a v1.1.0 -m "Pixel Funds v1.1.0"
   git push origin v1.1.0
   ```
   Alternatively, open **Actions → Release APK → Run workflow** on GitHub and enter the tag name (e.g. `v1.0.0`).

Once complete, the workflow publishes a GitHub Release with:
- `Pixel-Funds-<tag>.apk` (installable Android APK)
- `Pixel-Funds-<tag>.apk.sha256` (SHA-256 verification file)
- Auto-generated release notes and install instructions

---

## Optional Repository Secrets (Recommended for Production)

The release workflow works out of the box with zero configuration (using a debug-signed APK fallback). To publish **production-signed APKs** with **Cloud Sync / Google Sign-In** enabled, add these secrets under **Settings → Secrets and variables → Actions**:

| Secret | Description | How to Generate |
| :--- | :--- | :--- |
| `KEYSTORE_BASE64` | Base64-encoded `.jks` upload keystore (`upload` alias) | `base64 -w 0 my-upload-key.jks` |
| `STORE_PASSWORD` | Keystore password | Your `.jks` store password |
| `KEY_PASSWORD` | Key password for the `upload` alias | Your `.jks` key password |
| `GOOGLE_SERVICES_JSON_BASE64` | Base64-encoded `app/google-services.json` for Firebase Auth / Firestore | `base64 -w 0 app/google-services.json` |

> [!TIP]
> Using a consistent `KEYSTORE_BASE64` across releases lets users upgrade directly over an existing install without uninstalling first.
