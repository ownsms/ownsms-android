# ownsms-android

Native Android (Kotlin) **sender** app for [ownsms](https://ownsms.uz): registers a phone with an
ownsms backend, long-polls for queued SMS, and sends them through the phone's own SIM.

> Status: **beta.** Build it in Android Studio and run on a **real device** (the SMS-via-SIM path
> cannot be exercised on most emulators).

## What's implemented

- **In-app registration + pairing** — register a new account/device (gets its device token + shows
  the API key), or join an existing account with a pairing code.
- Foreground service running an HTTP **long-poll** loop against the device protocol.
- **SmsManager** sending with **dual-SIM** selection (`subscriptionId`), multipart-safe.
- Delivery results via PendingIntent receiver → local job state (`sent` / `delivered` / `failed`).
- **At-most-once**: a job left in-flight across a crash is failed, never resent (Room reconcile).
- **Per-SIM config sync** (`GET /device/config`) — enforces rate limits (min/hour/day), jitter,
  **working hours**, and **daily quota** (auto-pause).
- **Reliability**: boot-restart, **WorkManager watchdog**, battery-optimization request,
  **OEM-autostart** deep-links, `POST_NOTIFICATIONS` runtime request, and an onboarding
  **health-check** (per-requirement ✓/✗).
- **Multi-screen Material 3 UI** — bottom-nav (Home / Activity / SIMs / Settings) + per-screen
  TopAppBar, onboarding gate, selectable API key.
- **Developer API + test-send** — the app calls `POST /api/v1/messages` with its API key to fire a
  test SMS end-to-end (verifies server → device → SIM).
- **Server-backed dashboard** — Activity toggles between this device's local history and the
  account-wide messages + device status pulled from the server (`GET /api/v1/messages`,
  `GET /api/v1/device`), with stat tiles, filters, message detail, and loading states.
- **Localized** — all UI strings in resources; Uzbek default + **Russian** (`values-ru`).
- **Release-ready** — R8 minify + resource shrink with keep rules for the reflective Moshi/Retrofit/
  Room stack, adaptive launcher icon, `versionCode`/`versionName`.
- **Tests + CI** — JVM unit tests (rate/working-hours gate), an instrumentation smoke scaffold
  (`androidTest`), **ktlint**, and a **GitHub Actions** workflow (ktlint + unit tests + assemble).

Out of scope (next iteration): campaigns authoring UI, on-device instrumentation runs (the scaffold
compiles but needs a device/emulator), a real signing keystore for Play upload.

## Requirements

- **Android Studio** (Koala/2024.1+), which bundles **JDK 17**.
- An **Android device, API 26+ (Android 8)**, with an active SIM and mobile balance.
- An **ownsms backend** reachable from the phone (the cloud, or self-hosted `ownsms` Django app,
  or `localhost` exposed via **ngrok/jprq**).

## Build & run

1. **Open** the project folder in Android Studio. On first sync it downloads Gradle 8.7 + the
   Android Gradle Plugin. The Gradle wrapper (`gradlew` + `gradle/wrapper/gradle-wrapper.jar`) is
   committed, so `./gradlew assembleDebug` works from a clean checkout / CI.
2. **Run** on a connected device (`Shift+F10`) or `./gradlew installDebug`.
   For a shrunk build: `./gradlew assembleRelease` (debug-signed until a real keystore is configured).

## First-time setup on the device

1. Install + open the app.
2. Tap **SMS / Telefon / Bildirishnoma ruxsati** and grant all.
3. Tap **Batareya optimizatsiyasini o'chirish** and allow.
4. On Xiaomi/Huawei/Oppo/Vivo: open app settings and enable **Autostart** manually.
5. Enter the **API URL** (your ngrok `https://...` or `https://api.ownsms.uz`), tap **URL saqlash**.
6. Tap **Yangi qurilma ro'yxatdan o'tkazish** — the app registers, stores its device token, and
   shows the **API KEY** for your server. (To add a second phone to the same account, tap
   **Yangi qurilma uchun pairing kod** on the first phone, then enter that code under
   **Pairing kod** on the second.)
7. Pick the default **SIM**, then tap **Boshlash**.

### Using it

Copy the shown **API KEY** to your application server and send SMS with
`POST /api/v1/messages` (Bearer API key); the phone delivers them. For local testing, run the
`ownsms` Django backend, expose it with ngrok, and use that URL in step 5.

## Module map

```
app/src/main/java/uz/ownsms/sender/
  OwnSmsApp.kt · ServiceLocator.kt        # app + tiny DI
  data/remote/   ApiModels, OwnSmsApi, DevApi, ApiClient  # device protocol + developer API (api key)
  data/db/       JobEntity, JobDao, AppDatabase     # Room local job queue
  data/prefs/    Settings                           # base URL, token, api key, default SIM
  sms/           SimRepository, SmsSender, SmsResultReceiver
  reliability/   ReliabilityChecker                 # onboarding health-check
  service/       SenderService, BootReceiver, Notifications, Pacing,
                 RateGate, OemAutostart, WatchdogWorker
  ui/            AppRoot (nav+scaffold), MainViewModel, theme/, components/,
                 screens/ (Onboarding, Home, Activity, Sims, Settings)
app/src/main/res/  values/strings.xml · values-ru/strings.xml · adaptive launcher icon
app/src/test/         RateGateTest                   # JVM unit tests
app/src/androidTest/  AppSmokeTest                   # instrumentation scaffold (needs a device)
app/proguard-rules.pro · .editorconfig (ktlint)      # release keep rules + lint config
.github/workflows/android.yml                        # CI: ktlint + unit tests + assemble
```
