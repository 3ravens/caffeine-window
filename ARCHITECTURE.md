# Architecture — Caffeine Window

This doc exists to answer one question unambiguously: **does this code go in the public repo or the private one?** Read this before adding a new module, class, or dependency.

## Why the split exists

The app's trust story depends on the free tier being fully open and auditable — anyone can verify that wake detection and notifications run entirely on-device with no data collected. The Pro tier (wearable integration) is the paid feature, so its implementation and the purchase-verification logic stay closed — otherwise the entitlement check is trivially patchable out of an open binary.

## The two repos

```
caffeine-window/              (public — MIT)
└─ shared/                    KMP library (androidTarget today; iosMain later)
    ├─ wake/
    │   └─ WakeSignalSource.kt        ← interface only, no paid implementations
    ├─ repository/
    │   ├─ WakeRepository.kt
    │   └─ SettingsRepository.kt
    ├─ scheduler/
    │   ├─ NotificationScheduler.kt   ← interface + DayBoundary / SchedulingMath
    │   └─ …
    ├─ settings/
    │   └─ FeatureFlags.kt            ← FeatureFlag enum + FreeTierFeatureFlagReader
    └─ db/                            ← Room schema, DAOs (expect/actual DB constructor)
androidApp/
    ├─ ui/                            ← Compose screens
    ├─ signal/
    │   └─ ScreenUnlockSignalSource.kt
    ├─ notification/
    │   └─ AndroidNotificationScheduler.kt
    └─ service/                       ← FGS + boot/unlock receivers

iosApp/                       (Phase 3 — not present yet)
    ├─ ui/                            ← SwiftUI screens
    └─ signal/
        └─ AppForegroundSignalSource.swift

caffeine-window-pro/           (private)
├─ androidPro/
│   ├─ signal/
│   │   └─ HealthConnectSignalSource.kt
│   └─ billing/
│       └─ PlayBillingEntitlementVerifier.kt
└─ iosPro/
    ├─ signal/
    │   └─ HealthKitSignalSource.swift
    └─ billing/
        └─ StoreKitEntitlementVerifier.swift
```

Phase 1 reality: platform signal sources and notification scheduling live in `androidApp/` as ordinary implementations of shared interfaces. Only the Room database constructor uses `expect`/`actual` today. `iosApp/` is aspirational until Phase 3.

## The rule, in one sentence

**Anything that implements `WakeSignalSource` using a wearable API, and anything that checks or grants purchase entitlement, lives in the private repo. Everything else is public.**

## How the private code reaches the public app

The private repo is pulled in as a build-time dependency (private Maven/Gradle package, or a private Swift package), not copy-pasted in and not committed to the public repo's history at any point — including in branches, drafts, or reverted commits. Treat "never touches public git history" as equivalent to "never touches the public repo."

The public app only ever depends on the `WakeSignalSource` interface and a `FeatureFlagReader`. It doesn't import `HealthConnectSignalSource` or any billing class directly — DI wiring that binds the private implementations happens inside the private package's own module, injected at app-assembly time. The public free build binds `FreeTierFeatureFlagReader` (all flags off). The public `androidApp`/`iosApp` targets shouldn't need to change when a new Pro feature is added; only the private repo's DI module does.

## What's explicitly NOT allowed in the public repo

- Any class implementing `WakeSignalSource` against Health Connect, HealthKit, or any third-party wearable SDK
- Any purchase/entitlement verification logic, API keys, or store-specific billing code
- Hardcoded behavior that branches on Pro status beyond reading a `FeatureFlagReader` value (the *decision* of what counts as Pro-entitled lives in the private repo; the public repo only reads a flag someone else set)
- TODOs, comments, or dead code referencing unbuilt Pro features in implementation detail (a roadmap link is fine; pseudocode for `HealthKitSignalSource` is not)

## What's explicitly fine in the public repo

- The `WakeSignalSource` interface itself — the contract is public even though some implementations aren't
- The `FeatureFlag` enum listing flag names (e.g. `pro_wearable_signal`) — knowing a flag exists isn't a vulnerability; verifying it is what stays private
- All free-tier UI, scheduling, and storage code
- Tests for everything above, including a fake/no-op `WakeSignalSource` implementation used in public-repo tests (it should not resemble the real wearable integration)

## When you're not sure

If a change touches wearables, health data permissions, billing, or purchase state in any way, default to the private repo and ask before committing to the public one. Misplacing something in the wrong direction once is a quick fix; misplacing private logic into public git history is not reversible by deleting the file — the history still has it.
