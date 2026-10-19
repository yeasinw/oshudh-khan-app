# Oshudh Khan

An Android app that reminds you to take your medicine on time and keeps track of the doses you've taken.

## Features

- **Medicine reminders**: add a medicine with its dosage and schedule it daily (1–4 times a day) or every N days. Reminders use exact alarms and are restored after the phone restarts.
- **Edit and pause**: change a medicine at any time, or switch its reminders off without deleting it.
- **Actionable notifications**: each reminder has two buttons, **Taken** and **Remind in 15 min**. A reminder is skipped if that dose has already been taken.
- **Home dashboard**
  - A greeting based on the time of day
  - A heatmap of the last 7 days, shaded by doses taken out of doses scheduled
  - Today's doses, each with a **Take** button, an *Overdue* label when the time has passed, and the time it was taken. After tapping Take you get a few seconds to **Undo**.
- **Profile**: name, age and email, stored on the device.
- **Floating glass navigation bar**: icon-only tabs, where the selected tab expands to show its label, and a round **+** button to add a medicine. The bar blurs the content behind it on Android 12+.

## Tech stack

| Area | Library |
| --- | --- |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Local data | Room (medicines, dose log), DataStore Preferences (profile) |
| Reminders | AlarmManager exact alarms, BroadcastReceivers, NotificationCompat |
| Glass effect | [Haze](https://github.com/chrisbanes/haze) |
| Dates | `java.time` (via core library desugaring) |

Requirements: min SDK 24, target/compile SDK 36, Kotlin 2.3, AGP 9.

## Project structure

```
app/src/main/java/com/yeasinx/oshudhkhan/
├── data/            Room entities and DAOs (Medicine, DoseLog), repositories,
│                    DoseSchedule (which doses are due on a given date), UserRepository
├── notification/    AlarmScheduler, AlarmReceiver, DoseActionReceiver (Taken / snooze),
│                    BootReceiver, NotificationHelper
├── navigation/      NavGraph, floating glass bottom bar
├── ui/
│   ├── screens/     Home, Medicine list, Add/Edit medicine, Profile
│   ├── components/  MedicineCard, WeekHeatmap
│   ├── home/        HomeViewModel
│   ├── medicine/    MedicineViewModel
│   ├── profile/     ProfileViewModel
│   └── theme/
├── MainActivity.kt
└── MedicineApp.kt   Creates the database and repositories, and the notification channel
```

## Getting started

1. Open the project in Android Studio.
2. Build and run the `app` configuration, or from the command line:

   ```bash
   ./gradlew :app:assembleDebug        # build a debug APK
   ./gradlew :app:testDebugUnitTest    # run unit tests
   ```

3. On first launch, allow notifications. On Android 12+, the app also needs permission to schedule exact alarms, or reminders won't fire.

## Releases (CI)

Every push to `main` runs [`.github/workflows/release.yml`](.github/workflows/release.yml). It runs the unit tests, builds a signed release APK and publishes it as a GitHub Release (`v1.0.<run number>`) with auto-generated notes. It can also be started by hand from the Actions tab.

- **Versioning:** `versionCode` is the workflow run number and `versionName` is `1.0.<run number>`. To move to `2.0`, change the prefix in `app/build.gradle.kts`.
- **Signing:** the APK is signed with the existing release keystore, supplied through these repository secrets:

  ```bash
  gh secret set KEYSTORE_BASE64 < <(base64 -i /path/to/release.jks)
  gh secret set KEYSTORE_PASSWORD
  gh secret set KEY_ALIAS
  gh secret set KEY_PASSWORD
  ```

  The release keystore is kept outside the repo, in `~/.android-keys/oshudhkhan/` on the maintainer's machine. Keep a backup of it: if the key is lost, installed copies of the app can never be updated.

  Local builds don't need these. Without `KEYSTORE_PATH`, `assembleRelease` produces an unsigned APK.

## Permissions

| Permission | Why |
| --- | --- |
| `POST_NOTIFICATIONS` | Show medicine reminders (Android 13+) |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Fire reminders at the exact time |
| `RECEIVE_BOOT_COMPLETED` | Reschedule reminders after the phone restarts |
| `WAKE_LOCK` | Deliver alarms while the device is idle |

## Roadmap

- [ ] Firebase Auth and Cloud Firestore for the user profile (`UserRepository` is the single place that will change)
- [ ] Longer adherence history (monthly view)
