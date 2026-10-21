# StarClone — Project Instructions

## Overview

Fork of Round-Sync (Android rclone wrapper), renamed to **StarClone**.
Package: `ch.jacem.starclone` (applicationId + namespace).

## Build & Run

```bash
# Kotlin compilation check
./gradlew app:compilePlaystoreDebugKotlin

# Java compilation check
./gradlew app:compilePlaystoreDebugJavaWithJavac

# Install on connected device
./gradlew app:installPlaystoreDebug
```

- Product flavors: `playstore` (Google Play AAB, no self-update), `oss` (GitHub releases and F-Droid style repositories,
  optional update check against GitHub releases, `REQUEST_INSTALL_PACKAGES` only here). Same applicationId and version
  codes. `BuildConfig.SELF_UPDATES` tells them apart.
- rclone compiled as native .so via Go+NDK (version in `gradle.properties`)

## Architecture

- **Mixed Java + Kotlin** codebase, Jetpack Compose added incrementally (Material 3 + dynamic colors)
- **Bottom nav** with ViewPager2 (5 tabs: Remotes, Tasks, Processes, Logs, Settings)
- **Fragment wrapper pattern**: each Compose screen has a Fragment hosting `ComposeView` + `StarCloneTheme`
- **Container fragments**: `RemotesContainerFragment` and `SettingsContainerFragment` manage child fragment navigation within their tab
- **Activity shell pattern**: `activity_compose_shell.xml` (toolbar + ComposeView) reused by standalone activities

### Key Directories

| Path | Purpose |
|------|---------|
| `ui/theme/Theme.kt` | `StarCloneTheme` — dynamic colors (API 31+), AMOLED mode, static green fallback |
| `ui/components/` | Reusable: StarCloneCard, StarCloneTopBar, StarCloneBottomSheet, StarCloneEmptyState, StarCloneListItem, StarCloneDialog |
| `ui/screens/` | All Compose screens organized by feature |
| `ui/navigation/` | Bottom nav, ViewPager adapter, nav routes |
| `Fragments/` | Legacy fragment wrappers (FileExplorerFragment, etc.) |
| `Activities/` | Legacy XML activities (TaskActivity, TriggerActivity, etc.) |
| `Dialogs/` | Legacy Java dialog classes (being migrated to Compose) |

## Releases

- **kidager-bot** (release-please as a GitHub App, `release-please-config.json`, `version.txt`, the
  `x-release-please-version` marker in `app/build.gradle`) opens the release PR on every push to `main`; merging it
  creates the release. versionCode = `(major·1000000 + minor·1000 + patch)·10`, the last digit for the split APKs' ABI.
- The `build` label on the release PR builds the signed playstore AAB and the universal playstore/oss APKs (`release-build.yml`, its
  `release-build` check blocks the merge until then); `release.yml`
  reuses them: APKs on the GitHub release, the AAB to Play internal (keyless: Workload Identity Federation), notes
  from the release PR's comment (Gemini writes them, editable). `promote.yml` (manual) moves internal to production.
- `apk-debug` / `apk-release` labels on a PR build its playstore universal APK and link it in a comment.
- rclone is compiled only when its version, Go, the NDK or `rclone/build.gradle` change (`.github/actions/rclone`,
  cached; `-x :rclone:buildAll` on a cache hit).
- Upload key: Actions secrets `UPLOAD_KEYSTORE_BASE64`, `KEY_PASSWORD`, `STORE_PASSWORD`, variable `KEY_ALIAS`; local
  copy `.my.upload-keystore.jks` + `.my.release-secrets.env` (gitignored). Locally, `signing.properties` still works.

## Coding Rules

### KISS, SOLID, DRY (Non-negotiable)

- Write the simplest code that solves the problem
- Prefer flat over nested — avoid deep callback chains
- No premature abstraction — three similar lines are better than one clever generic function
- No feature flags, extension points, or configurability unless explicitly requested
- Each class/file does ONE thing
- Extract repeated logic only when you have 3+ occurrences (Rule of Three)

### Compose Rules

- **Standard Material 3 only** — never create custom buttons, text fields, cards, dialogs, toolbars, or navigation bars
- **No flashy animations** — no bounce, spring, parallax, animated gradients, or pulsing effects
- **Always use** `MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`, `MaterialTheme.shapes.*`
- **Never hardcode** colors, text sizes, or dimensions
- **StarCloneListItem**: use `containerColor = Color.Transparent` to avoid white rectangles on grey backgrounds
- **LazyColumn**: always provide `key = { it.id }` and `contentType` for recycling
- **Mark data classes** with `@Immutable` when passed to composables
- **Pre-compute formatting** (dates, strings) in ViewModel, not during composition
- **Debug builds** are 2-5x slower than release for Compose — lag may be debug-only

### Dialogs

- All new dialogs must use the `StarCloneDialog` component (`ui/components/StarCloneDialog.kt`)
- Legacy Java dialogs (in `Dialogs/`) use `RoundedCornersDialog` XML style — migrate to Compose when touched

### JVM Setter Clash Fix

Use `updateSearchQuery()` / `updateSortOrder()` instead of `setX()` with `mutableStateOf` to avoid JVM setter conflicts.

### Dynamic Colors

`ActivityHelper.applyTheme()` applies both dark mode and `DynamicColors.applyToActivityIfAvailable()` for all XML activities. Compose screens get dynamic colors via `StarCloneTheme`.

## Git & Versioning

- **Release Please** for automated versioning (conventional commits drive version bumps)
- **Never mention Claude, AI, or Co-Authored-By** in commit messages or PR descriptions
- **Always use** conventional commits: `<type>(<scope>): <description>`
- **Always include scope** when possible
- **One-line commits** — no body/footer, no multi-line messages
- **Never commit** `.claude/plans/.my.*` files

## Navigation

- Bottom nav replaces drawer navigation (Compose `NavigationBar`)
- ViewPager2 with `MainPagerAdapter` manages tab pages
- Tab switching via bottom nav buttons is instant (no animation)
- Swiping between tabs is enabled
- FileExplorerFragment contributes menu items to Activity toolbar via `setHasOptionsMenu(true)`

## SAF (Storage Access Framework)

- Custom FilePicker deleted, replaced with Android SAF (`OpenMultipleDocuments`, `OpenDocumentTree`)
- `util/UriUtils.kt` resolves SAF URIs to filesystem paths
- `VirtualContentProvider` enabled by default (rclone remotes appear in SAF picker)

## Workers

- `EphemeralWorker` / `SyncWorker`: exit code checked (`FAILURE_REASON.RCLONE_ERROR` set on non-zero)
- `onStopped()`: only logs cancellation if `sRcloneProcess != null`
- `finishWork()`: `unregisterReceiver` wrapped in try-catch

## Backlog

See `.claude/plans/.my.backlog.md` for current bugs and planned work.

## Do NOT

- Run `git commit` directly — use `/git-commit` skill
- Run `gh pr create` directly — use `/pr-summary` skill
- Run `larastan` / static analysis without asking first
- Add docstrings, comments, or type annotations to code you didn't change
- Add error handling for scenarios that can't happen
- Create documentation files unless explicitly requested
- Over-engineer — only make changes that are directly requested
