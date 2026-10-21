# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Oshudh Khan is a single-module Android app (`:app`, Kotlin, Jetpack Compose, Material 3) for medicine reminders and dose tracking, with optional Firebase sign-in and cloud backup. Package: `com.yeasinx.oshudhkhan`. Min SDK 24, target/compile 36, AGP 9, Kotlin 2.3, Java 11 with core library desugaring (`java.time` is used everywhere).

## Commands

```bash
./gradlew :app:assembleDebug                      # debug APK
./gradlew :app:installDebug                       # install on a connected device/emulator
./gradlew :app:testDebugUnitTest                  # JVM unit tests (app/src/test)
./gradlew :app:testDebugUnitTest --tests "com.yeasinx.oshudhkhan.CloudSyncTest"            # one class
./gradlew :app:testDebugUnitTest --tests "com.yeasinx.oshudhkhan.CloudSyncTest.newerEditWins" # one test
./gradlew :app:lintDebug                          # Android lint
./gradlew :app:assembleRelease                    # unsigned unless KEYSTORE_PATH etc. are set
```

`adb` is not on the default shell PATH; it lives at `~/Library/Android/sdk/platform-tools/adb`. Useful log tags: `CloudSync`, `CloudBackup`.

Unit tests are plain JVM tests. Android framework calls (e.g. `Patterns.EMAIL_ADDRESS`) are stubs there, so keep testable logic in pure functions (see `DoseSchedule`, `reconcile`, the record mappers in `CloudBackup.kt`). The instrumented tests are only template stubs.

## Build / config

- Dependencies go through the version catalog `gradle/libs.versions.toml`. Firebase uses the BoM (`firebase-bom`) with `firebase-auth` and `firebase-firestore`. Don't add `-ktx` artifacts; the Kotlin APIs live in the main artifacts on BoM 34+.
- `app/google-services.json` is required to build (google-services plugin) and is gitignored. CI recreates it from the base64 secret `GOOGLE_SERVICES_JSON`.
- Versioning: `versionCode` = `-PbuildNumber` (the CI run number), `versionName` = `1.0.<buildNumber>`. Release signing reads `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` from the environment.
- `.github/workflows/release.yml`: every push to `main` checks the required secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `GOOGLE_SERVICES_JSON`), runs unit tests, builds a signed APK and publishes the GitHub Release `v1.0.<run number>`.
- Work happens on feature branches and goes through PRs into `dev`/`main`. Commit messages use conventional prefixes (`feat:`, `ci:`, `docs:`, `chore:`).

## Architecture

**Manual DI.** `MedicineApp` (the Application class) lazily builds the Room database, `CloudBackup`, `MedicineRepository`, `UserRepository`, `AuthRepository` and `CloudSync`. `MainActivity` creates every ViewModel with its hand-written `*ViewModelFactory` and passes them into `MedicineNavGraph`. There is no Hilt/Koin. New dependencies are wired the same way. Broadcast receivers reach the repositories through `context.applicationContext as MedicineApp`.

**Navigation.** `navigation/NavGraph.kt` defines the routes as `sealed class Screen`: `home`, `medicine_list`, `add_medicine?medicineId=` (add and edit share one route; `-1` means add), `profile` and `sign_in`. `FloatingBottomNavBar` is a Haze-blurred glass bar drawn over the content. Screens reserve `bottomPadding` (`NAV_BAR_SPACE` + system nav inset) so content can scroll under it. The bar hides itself on full-screen forms (add/edit, sign in) by checking the current route.

**Local data (source of truth).**
- Room `MedicineDatabase` holds `medicines`, `dose_logs` and `tombstones`, with explicit migrations and no destructive fallback. Any schema change needs a new `Migration` added in `getDatabase`. `exportSchema = false`.
- `Medicine.id` is a local auto-increment Int. It is baked into alarm request codes (`AlarmScheduler.requestCode = id * 100 + reminderIndex`, snooze = `+50`), notification IDs and `DoseLog.medicineId`, so it must never change for an existing row.
- `Medicine.uuid` is the cross-device identity. `DoseLog` is unique on (`medicineUuid`, `date`, `time`) and deliberately has no foreign key: dose history outlives deleted medicines. A restored log whose medicine no longer exists gets `medicineId = 0`.
- `reminderTimes` is a comma-separated `HH:mm` string. Dates are stored as `yyyy-MM-dd` strings.
- `DoseSchedule` (pure logic) decides which doses are due on a date, for DAILY vs EVERY_N_DAYS.
- The profile lives in Preferences DataStore (`UserRepository`), not Room.

**Reminders.**
- `AlarmScheduler` sets one exact alarm (`setExactAndAllowWhileIdle`) per reminder time, aligned with `DoseSchedule.nextDueDate`.
- `AlarmReceiver` shows the notification and schedules the next occurrence itself. There are no repeating alarms.
- `DoseActionReceiver` handles the Taken and snooze buttons. `BootReceiver` reschedules all active medicines.
- Whoever changes a medicine is responsible for its alarms: `MedicineViewModel` for user edits, `CloudSync.applyMedicine` for changes that arrive from the cloud. Always cancel with the *old* medicine before rescheduling, because the reminder slots may have changed.

**Auth and cloud backup** (Firebase; sign-in is optional, email/password only).
- `AuthRepository` exposes `currentUser` as a Flow, built from `AuthStateListener`.
- `MedicineApp.onCreate` collects it and runs `CloudSync.sync(uid, email)` on sign-in and on every launch while signed in.
- Firestore layout: `users/{uid}` holds the profile fields; `users/{uid}/medicines/{uuid}`; `users/{uid}/doseLogs/{medicineUuid}_{date}_{time}`. Security rules allow only `request.auth.uid == uid` under `users/{uid}/**`.
- **Write-through:** `MedicineRepository` and `UserRepository` stamp `updatedAt` on every write and push to `CloudBackup`. Pushes are fire-and-forget: they're never awaited, Firestore queues them offline, and they're a no-op while signed out. A delete writes a local `Tombstone` and pushes a `deleted: true` record, never a hard delete.
- **Merge:** `reconcile()` in `CloudSync.kt` is last-writer-wins per item over `SyncRecord`s (live items plus tombstones). A delete wins a timestamp tie. A dose's `updatedAt` is its `takenAt`.
- Changes coming from the cloud are applied through the DAOs directly, not the repositories, so they are not re-stamped or re-pushed.
- **Account ownership:** `CloudSync` remembers `owner_uid` (DataStore `sync_state`). Signing in as a different account wipes local data and alarms before merging. Data created before any sign-in is adopted by the first account.
- **Profile email:** it is prefilled from the auth email inside `CloudSync`, *after* the merge. Doing it earlier would stamp a newer, mostly empty profile over the backed-up one.
