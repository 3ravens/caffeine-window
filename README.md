# Caffeine Window

A local-only Android app that detects when you wake up (via your first screen unlock of the day) and notifies you once your caffeine delay window opens.

Everything runs on-device. No backend, no analytics, no data collection. Crash reporting can be added later via Firebase Crashlytics (see below) — it is optional and limited to crash/error data.

## How it works

1. A low-priority foreground service listens for `ACTION_USER_PRESENT` (screen unlock).
2. The first unlock since the configured day boundary (default 3:00 AM) is stored as today's wake event.
3. When wake time + delay (60/90/120 min) elapses, a local notification fires.
4. The home screen shows idle, countdown, or "window open" state.

## Project structure

```
shared/          Kotlin Multiplatform — wake interface, repository, Room DB, scheduling math
androidApp/      Jetpack Compose UI, Hilt DI, Android signal source, notifications
```

This repo is the free tier. A future Pro tier (wearable sleep data as the wake signal) will live in a separate private repo.

## Build

Requirements: Android Studio Ladybug or newer, JDK 17.

```bash
./gradlew :androidApp:assembleDebug
```

Install the APK from `androidApp/build/outputs/apk/debug/`.

On first launch the Home screen prompts for reliability setup. Grant:

- **Notifications** (Android 13+)
- **Exact alarms** (for precise timing through Doze)
- **Battery optimization exemption** (OEM skins often kill background receivers without it)

You can also manage these under **Settings**.

## Deferred: Firebase Crashlytics

The app includes a `CrashReporter` interface with a no-op implementation. To wire Crashlytics later:

1. Create a Firebase project and add `google-services.json` to `androidApp/`.
2. Add the Google Services and Crashlytics Gradle plugins to the root and `androidApp` build files.
3. Implement `CrashReporter` with `FirebaseCrashlytics.getInstance()` and bind it in Hilt instead of `NoOpCrashReporter`.

Do not commit `google-services.json` if it contains project secrets you want to keep private — use local-only copies or CI secrets.

## License

MIT — see [LICENSE](LICENSE).
