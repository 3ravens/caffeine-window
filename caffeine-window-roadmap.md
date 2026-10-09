# Caffeine Window — build & launch roadmap

A phased plan: Android MVP → publish → iOS → wearable integration (paid tier).
Each phase ships something usable on its own — you're never blocked waiting for a "big bang" release.

**Repo model:** open-core. The free tier (everything through Phase 3) lives in a public repo. The Pro tier — wearable signal sources and the IAP entitlement check (Phase 4) — lives in a separate private repo. See `ARCHITECTURE.md` for the exact split.

---

## Phase 1 — Android base app (free tier, public repo)

**Goal:** working app that detects wake-up via screen unlock and notifies you when the caffeine window opens.

**Build tasks**

- [x] Set up project: Kotlin, Jetpack Compose, Hilt, Room (KMP `shared/` + `androidApp/`)
- [x] `WakeSignalSource` interface + `ScreenUnlockSignalSource` (`BroadcastReceiver` on `ACTION_USER_PRESENT` via foreground service)
- [x] "First unlock since day boundary" logic (e.g. resets at 3am, not midnight, so late-night unlocks don't miscount)
- [x] Room database: wake events table, settings table (delay duration, notification copy), feature-flag table
- [x] `NotificationScheduler` using `AlarmManager` (exact alarm with inexact fallback)
- [x] Compose UI: home screen (countdown / "window is open" state), settings screen (delay chips: 60/90/120 min)
- [x] Request battery-optimization exemption prompt (Android kills background receivers aggressively on many OEM skins — Samsung, Xiaomi especially) — without this, reliability will be inconsistent
- [x] Local notification with permission request flow (Android 13+ requires runtime notification permission)
- [ ] Basic analytics-free crash reporting (Firebase Crashlytics is free and doesn't count as "running a server" — it's a managed SDK) — deferred; `CrashReporter` + `NoOpCrashReporter` scaffold only
- [x] Make the repo public (MIT license, README explaining the local-only/no-data-collected design) — this is the trust signal worth leaning on in your store listing copy

**Definition of done:** install on your own phone, use it daily for a week, notification fires reliably even after phone has been idle/locked overnight.

**Rough effort:** 1–3 weekends for someone comfortable with Kotlin/Compose; longer if this is your first Android app.

---

## Phase 2 — Publish to Google Play

**Goal:** real users, real reviews, validate the core loop before investing in iOS or wearables.

**Tasks**

- [ ] Google Play developer account ($25 one-time)
- [ ] Privacy policy page (required even for a server-less app — covers what's stored locally and that nothing is transmitted). A free static page (GitHub Pages, Notion public page) is enough.
- [ ] Store listing: screenshots, short/long description, icon, feature graphic (app name: Caffeine Window)
- [ ] Data safety form in Play Console (declare: no data collected/shared, since everything is local)
- [ ] Closed testing track first (10–20 friends/family) before public release — catches OEM-specific battery-kill issues you can't find alone
- [ ] Public release
- [ ] Set up a lightweight feedback channel (in-app "send feedback" mailto link is enough — no backend needed)

**Definition of done:** app live on Play Store, at least a few real users outside your testing group, no major crash reports.

**Decision point:** let this run for 2–4 weeks before starting Phase 3. If usage/retention is weak, it's cheaper to iterate on Android than to have already built iOS in parallel.

---

## Phase 3 — iOS app (free tier, public repo)

**Goal:** same core experience on iOS, accepting the "first app open" compromise instead of true background unlock detection (see note below).

**Tasks**

- [ ] Apple Developer account ($99/year)
- [ ] iOS target / `iosApp`: shared module already exists from Phase 1 — add `iosMain` expect/actual (or Swift wrappers) for notifications and wake signal; do not re-extract shared logic
- [ ] SwiftUI screens mirroring the Android UI (home, settings)
- [ ] iOS wake-signal substitute: trigger off first app foreground/open of the day (iOS doesn't expose background unlock detection to third-party apps)
- [ ] Local notifications via `UNUserNotificationCenter`
- [ ] Notification permission request flow
- [ ] App Store privacy "nutrition label" (declare no data collected, matching Android's data safety form)
- [ ] TestFlight beta (same friends/family group)
- [ ] App Store submission and review

**Definition of done:** iOS app live, feature parity with Android free tier.

**Architecture note:** Phase 1 already ships a KMP `shared/` module (repositories, Room, scheduling math, interfaces). Phase 3 is mostly "write SwiftUI + the iOS wake-signal / notification wrappers" against that shared code, not a second full extract.

---

## Phase 4 — Wearable integration (Pro unlock, private repo)

**Goal:** paid tier using Health Connect (Android) / HealthKit (iOS) sleep data as the wake signal instead of phone unlock.

**Tasks**

- [ ] Set up the private repo (or private submodule/package pulled into the public repo's build) — this is where everything below lives, not in the public repo
- [ ] `HealthConnectSignalSource` (Android) reading sleep session end times
- [ ] `HealthKitSignalSource` (iOS) equivalent
- [ ] Health data permission request flow on both platforms (these have stricter review scrutiny — budget extra App Store/Play review time)
- [ ] Feature-flag system in the database for the unlock (build this as an extensible flag set, not a single boolean, so future paid features slot in without a schema change)
- [ ] In-app purchase: Play Billing (Android), StoreKit 2 (iOS) — one-time unlock, not subscription
- [ ] Paywall screen: clear value prop tied to wearable accuracy ("more accurate than guessing from your first phone touch")
- [ ] Restore-purchase flow (required by both stores)
- [ ] QA across at least one real device per platform with a connected wearable (Oura/Fitbit/Garmin on Android via Health Connect; Apple Watch/Oura via HealthKit on iOS) — sleep data sync timing varies by brand, worth testing with whatever you personally have access to

**Definition of done:** Pro unlock purchasable on both platforms, wearable-based wake detection working and visibly more accurate than the free tier for testers who have a ring/watch.

---

## Phase 5 — Caffeine consumption tracking

**Goal:** let users log when they actually consume caffeine (and, later, how much and what kind), then feed that back into the notification timing so the "window" adapts to real behavior instead of a fixed delay.

**Why after Phase 4:** the core loop (wake detection → notification) needs to be validated and stable across both platforms first. Consumption logging is an additive feature on top of a working timer, not part of the MVP. It also benefits from the wake-event history table already retaining data, so the logging UI and stats can reuse that pattern.

**Build tasks (staged — ship the basic version first, expand later)**

- [ ] **Basic logging (v1):** one-tap "I had caffeine" action that records a timestamped consumption event. New Room table for consumption events (retain history like wake events — don't collapse to a single row), normalized through the repository layer, never written directly from UI.
- [ ] Surface the latest consumption event on the home screen (e.g. "last caffeine: 2h ago") so the log is visible, not write-only.
- [ ] **Amount + duration (v2):** optional fields for how much caffeine (mg, or a simple small/medium/large preset) and a rough "how long it'll last" estimate derived from amount. Keep the math in the shared module so both platforms stay consistent.
- [ ] **Drink type (v2):** optional drink category (coffee, tea, energy drink, etc.) — note that more artificial sources like energy drinks differ from coffee, so type can refine the duration estimate later. Store as an extensible enum/lookup, not hardcoded UI strings.
- [ ] **Effect feedback (v3):** let users record the perceived effect of a given dose/timing (e.g. felt it kick in, too jittery, too late) so the notification delay can be tuned per-user based on real outcomes rather than the static 60/90/120 default.
- [ ] Feed consumption + effect history back into `NotificationScheduler` timing as an optional adaptive adjustment, keeping the fixed-delay path as the default/fallback.

**Definition of done:** users can log caffeine intake in one tap, optionally enrich it with amount/type, and (v3) the notification timing reflects their recorded effects.

**Notes**
- Keep this fully on-device — consumption and effect data is personal behavioral data and must not leave the device (same constraint as everything else). This is local logging for the user's own benefit, not analytics.
- Decide free vs. Pro split per sub-feature: basic logging is a natural free-tier addition; adaptive timing from effect feedback could be a candidate Pro feature flag (slots into the Phase 4 flag set) — defer the call until there's usage data.

---

## Phase 6 — Caffeine cutoff reminder (bedtime clearance)

**Goal:** notify the user to stop consuming caffeine early enough that it has largely cleared their system by bedtime, using caffeine's half-life (~5–6 hours for most people).

**Why after Phase 5:** the basic version only needs a bedtime setting and fixed half-life math, but the feature gets meaningfully better once consumption logging exists — the app can factor in what the user already drank today and tell them whether another cup is still "safe" or they're past the cutoff. Building it after Phase 5 also keeps Phase 5 focused on the morning-side loop without splitting attention across two notification types.

**Build tasks (staged — ship the basic version first, expand later)**

- [ ] **Bedtime setting (v1):** user-configurable target sleep time (or a simple "I usually sleep around…" picker). Store in the existing settings table via the repository layer.
- [ ] **Cutoff notification (v1):** compute a "stop caffeine by" time from bedtime minus a configurable clearance buffer (default: ~10 hours — roughly two half-lives, so residual caffeine is negligible for most people). Schedule a second local notification via `NotificationScheduler`, idempotent alongside the existing wake-window notification.
- [ ] **Half-life / sensitivity tuning (v2):** let advanced users adjust the assumed half-life or clearance buffer (e.g. slower metabolizers, pregnancy, certain medications) — keep defaults conservative and copy in "try this protocol" framing, not medical advice.
- [ ] **Consumption-aware cutoff (v2, depends on Phase 5):** when the user has logged intake today, compute cutoff from their latest dose + amount (using Phase 5's mg presets) instead of a single fixed time — e.g. "you had 200 mg at 9am; skip caffeine after ~1pm if you're in bed by 10."
- [ ] Surface cutoff state on the home screen (countdown to cutoff, or "you're clear for sleep" once the buffer has passed).

**Definition of done:** users who enable the feature get a reliable evening reminder to stop caffeine in time for their chosen bedtime; with Phase 5 logging active, the cutoff reflects what they actually consumed that day.

**Notes**
- Fully on-device — bedtime and metabolism assumptions are personal and stay local.
- Two independent notifications (wake window open + caffeine cutoff) should share scheduling infrastructure but remain separately toggleable in settings.
- Decide free vs. Pro split: basic bedtime + fixed-buffer cutoff is a natural free-tier addition; consumption-aware personalized cutoff could pair with Phase 5's adaptive features as a Pro candidate — defer until there's usage data.

---

## Sequencing notes

- **Don't skip the Phase 2 pause.** The cheapest mistake to avoid here is building iOS and wearables before confirming people actually use the free Android version daily. A timer nobody opens after day 3 isn't worth a wearable integration.
- **KMP shared module already exists.** Keep platform-only code out of `shared/` so Phase 3 stays additive.
- **Pricing the Pro unlock:** worth waiting until you have Phase 2 usage data before locking in a number — you'll have a much better sense of willingness-to-pay once real people are using the free tier.
- **Keep the public/private boundary clean from day one.** Phase 1's `WakeSignalSource` interface is the seam the private repo plugs into later — don't let Pro-tier assumptions leak into the public repo's code now, even informally (e.g. commented-out Health Connect imports, TODOs referencing Pro logic). See `ARCHITECTURE.md`.

