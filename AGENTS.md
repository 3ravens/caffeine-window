# Agents.md — Caffeine Window

Context for any coding agent (Cursor, etc.) working in this repo. Read this before generating code.

## What this app does

"Caffeine Window" detects when the user wakes up and sends a local notification once a delay window has passed, so caffeine intake aligns with the (debated but popular) "wait 90 minutes after waking" theory. No claim is made in-app that this is scientifically settled — copy should stay in "try this protocol" framing, not "proven to improve X."

## Non-negotiable constraints

- **No backend, ever, for core functionality.** Everything runs on-device: local DB, local scheduling, local notifications. Do not introduce a server, API client, or cloud sync as part of normal feature work. If a task seems to require one, stop and flag it rather than building it.
- **No data leaves the device.** No analytics SDKs that phone home user behavior, no third-party trackers. Crash reporting (e.g. Crashlytics) is the one exception — it's a managed SDK, not a server we run, and should stay limited to crash/error data, not behavioral tracking. Crashlytics is deferred; the repo has a `CrashReporter` interface with a no-op implementation only.
- **Monetization is a single one-time IAP unlock, not a subscription.** Don't scaffold subscription billing, trial periods, or recurring entitlement checks. Build the unlock as a feature-flag set (see Data model), not a single boolean — multiple paid features may exist later.
- **Free tier must not depend on wearable/health permissions.** Notifications, exact alarms, foreground-service, boot, and battery-optimization exemption are required for reliable free-tier unlock detection on Android. Wearable/health permissions (Health Connect / HealthKit) are Pro-only — don't gate free-tier functionality behind them.
- **Open-core split: free tier is public, Pro tier is private.** This repo is open source and contains only free-tier code. `HealthConnectSignalSource`, `HealthKitSignalSource`, the IAP entitlement check, and any other Pro-only logic live in a separate private repo (see `ARCHITECTURE.md`). Never add Pro-tier code, IAP keys, or entitlement-verification logic to this repo — if a task seems to need that, stop and flag it rather than committing it here.

## Architecture

Core abstraction: wake detection is decoupled from everything downstream via one interface.

```kotlin
interface WakeSignalSource {
  fun observeWakeEvents(): Flow<WakeEvent>
}
data class WakeEvent(val timestamp: Instant, val source: String)
```

- `ScreenUnlockSignalSource` (free tier, public repo) — Android: `BroadcastReceiver` on `ACTION_USER_PRESENT` registered from a foreground service. iOS (Phase 3): first app-foreground event of the day (no true background unlock detection exists on iOS — don't try to build one).
- `HealthConnectSignalSource` / `HealthKitSignalSource` (Pro tier, **private repo, not here**) — sleep session end time from Health Connect (Android) / HealthKit (iOS). This repo should only ever see these referenced through the public `WakeSignalSource` interface, never their implementations.
- Only the active, entitled source(s) should be bound via DI (Hilt on Android). New signal sources must implement this interface — never bypass it to write directly to the repository.

Data flow is one-directional and layered — don't let UI talk to the database directly, and don't let signal sources write to the DB directly:

```
WakeSignalSource(s) → WakeRepository → Room DB + NotificationScheduler → local notification
                            ↑
              SettingsRepository / FeatureFlagReader
                            ↑
                       Compose UI (settings/history reads + writes here only)
```

- Repositories and feature-flag readers own DAO access. UI and signal sources never touch Room.
- `WakeRepository` normalizes events from whichever signal source(s) are active into one canonical "wake event for today." First wake of the logical day wins in the free tier. When Pro is active (Phase 4), prefer the wearable signal over screen-unlock if both arrive — that ranking is not implemented yet.
- `NotificationScheduler` is invoked by `WakeRepository` (and settings changes) with wake time + delay + copy; the Android impl uses `AlarmManager`. It must be re-triggerable/idempotent and must restore today's alarm when the monitor service starts (alarms die across reboot). Reschedule from **today's** wake only — never from a past-day latest row.

## Repo structure (open-core)

See `ARCHITECTURE.md` for the full public/private module breakdown. Short version: this repo is the public free tier. The Pro tier (wearable signal sources + IAP entitlement check) lives in a separate private repo and is pulled in at build time — it is not, and should never become, part of this codebase. If you're ever asked to implement something that touches Health Connect, HealthKit, or purchase verification, that work belongs in the private repo, not here.

## Cross-platform strategy

Targeting Kotlin Multiplatform (KMP):
- Shared module (`shared/`): `WakeSignalSource` interface, repositories, scheduling math, settings/feature-flag models, Room schema/DAOs. Already present for Phase 1.
- Platform-specific: signal source implementations, notification scheduling, and UI live in `androidApp/` today as ordinary interface implementations (not `expect`/`actual`, except the Room database constructor). iOS (`iosApp/`) is Phase 3 — do not scaffold it early.
- When adding a feature, default to writing it in the shared module unless it inherently requires a platform API. If unsure which side something belongs on, ask rather than guessing — misplaced platform code in `shared/` breaks the other platform's build.

## Reliability gotchas worth knowing

- Many Android OEM skins (Samsung, Xiaomi, Huawei) aggressively kill background receivers under battery optimization. The app must prompt for battery-optimization exemption (per-app request intent) and surface a clear Home/Settings explanation when reliability permissions are missing.
- Android 13+ requires runtime notification permission — handle the denied / permanently-denied case with an in-app explanation and a path to system settings, not a silent no-op.
- Exact-alarm permission (Android 12+) may fall back to inexact alarms; surface that in Settings and keep Home countdown honest about wall-clock targets.
- Health Connect/HealthKit permission flows are stricter and slower to review on both stores — any change touching these should include an accurate, specific permission-usage description, not boilerplate. Those changes belong in the private Pro repo.

## Data model conventions

- Settings and entitlement state: store the Pro unlock as a set of named feature flags (e.g. `pro_wearable_signal`, future flags), not a single `isPro: Boolean`. New paid features should add a flag, not branch on a global Pro check. The public free build binds `FreeTierFeatureFlagReader` (all flags off).
- Wake events table should retain history (for the future "stats" feature already on the roadmap), not just the latest value — don't optimize this into a single-row table. Same-day uniqueness is enforced with a unique index on `logicalDayStartEpochMillis`.

## Code style

- Kotlin: standard idiomatic style, prefer `Flow`/coroutines over callbacks, prefer immutable data classes.
- Compose: stateless composables + state hoisted to ViewModels; no business logic in composables.
- Swift/SwiftUI (once Phase 3 starts): mirror the shared module's behavior exactly — the iOS UI should not invent its own delay logic, notification copy, or settings defaults that diverge from the Android/shared version.
- No dead feature flags, no commented-out code left in commits.

## Current phase

Phase 1 (Android free-tier MVP) is largely implemented per `caffeine-window-roadmap.md`. iOS and wearable integration (Phases 3–4) are intentionally not started — don't preemptively scaffold Health Connect, HealthKit, IAP billing, or iOS targets unless explicitly asked. Keep the shared/KMP boundary clean now so those phases are additive later, not a rewrite.
