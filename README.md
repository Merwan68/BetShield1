# BetShield

**Take Back Control. Block the Bet.**

BetShield is a privacy-focused, offline-first digital-wellbeing Android application built to help users—especially in **Ethiopia** alongside international platforms—reduce impulsive gambling by blocking betting websites and applications locally on their device.

> **Implementation Note:** This repository implements BetShield natively for Android using **Kotlin, Jetpack Compose, Material Design 3, Android `VpnService`, Room, and Jetpack DataStore** (matching the AI Studio Android Gradle build environment). An iOS companion client can share the same `gambling_database.json` registry schema in future development.

---

## 1. Features

- **Home Dashboard:** Real-time local DNS shield status, one-tap Activate/Pause control, counts for filtered domains, restricted apps, total recorded blocking events, active categories, and instant access to Emergency Support.
- **Local VPN DNS Website Blocking (`BetShieldVpnService`):** Filters known Ethiopian (`betika.et`, `vamos.et`, `hulusport.com`, `winner.et`, `habeshabet.com`, `abyssiniabet.com`, etc.) and international gambling domains + custom user-added domains locally via UDP port 53 DNS `NXDOMAIN` responses—with **zero HTTPS interception**.
- **Opt-In Gambling App Guard (`BetShieldAccessibilityService`):** When explicitly authorized by the user in Android Accessibility Settings, detects foreground launches of verified gambling packages or user-selected installed apps and redirects to a calming cooling-off screen.
- **Structured Ethiopian & International Database (`gambling_database.json`):** Offline-first JSON registry with version tracking, domain aliases, category tags, and honest `verifiedDomain` / `verifiedPackage` flags (never fabricating unverified package IDs). Supports manual HTTPS remote updates.
- **Protection Center:** Category toggles, per-platform overrides, custom domain addition, live domain filter verification check, installed launcher app selector, and manual package ID entry.
- **Statistics & Activity History:** Daily, weekly, and monthly blocking charts, most frequently blocked targets, and a clear reminder that blocking counts reflect local DNS/app events—not guaranteed gambling attempts or money saved.
- **Emergency Support:** Guided breathing & configurable cooling-off timer, rotating grounding reminders, and links to recognized international support resources.
- **Settings & Bilingual UI:** Light, Dark, and System themes; instant **English / አማርኛ (Amharic)** language switching; notification controls; and privacy/limitation disclosures.

---

## 2. Technology Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose + Material Design 3 (`Plus Jakarta Sans` & `JetBrains Mono`)
- **Architecture:** Clean MVVM (`ViewModel`, `StateFlow`, Repository pattern, Constructor Injection)
- **Networking & Serialization:** `OkHttp` + `Moshi` (for HTTPS registry updates & JSON parsing)
- **Local Storage:** `Room` (blocking events & custom rules) + `Jetpack DataStore` (preferences)
- **Android Services:** `android.net.VpnService` (local DNS loop) + optional `AccessibilityService`
- **Testing:** JUnit 4 + Robolectric + Jetpack Compose UI Test

---

## 3. Project Structure

```text
app/src/main/
├── assets/gambling_database.json        # Bundled Ethiopian & international gambling registry
├── java/com/example/
│   ├── BetShieldApplication.kt          # AppContainer & notification channels
│   ├── MainActivity.kt                  # Edge-to-edge host & VpnService permission launcher
│   ├── BlockedInterceptActivity.kt      # Calming intercept screen for blocked apps
│   ├── data/
│   │   ├── local/                       # Room DAOs, DataStore, DatabaseLoader, BlockListMatcher
│   │   ├── model/                       # Moshi JSON models & Room entities
│   │   └── repository/                  # ProtectionRepository (offline + remote sync)
│   ├── service/
│   │   ├── DnsPacketParser.kt           # Pure-Kotlin IPv4/UDP/DNS parser & NXDOMAIN builder
│   │   ├── BetShieldVpnService.kt       # Local-only DNS filtering VpnService
│   │   ├── BetShieldAccessibilityService.kt # Opt-in window-change App Guard
│   │   └── ProtectionNotificationHelper.kt
│   └── ui/
│       ├── i18n/AppStrings.kt           # English & Amharic copy
│       ├── navigation/                  # Responsive NavigationBar / NavigationRail shell
│       ├── screens/                     # Onboarding, Dashboard, Protection, Stats, Emergency, Settings
│       └── theme/                       # BetShield Color, Type, Theme
└── res/                                 # Adaptive launcher icon, custom fonts, XML configs
```

---

## 4. Build & Installation Instructions

### Prerequisites (Developers)
- JDK 17+
- Android SDK (API 36) & Gradle 8.x+

### Local Commands
```bash
# Run unit & Robolectric tests
gradle :app:testDebugUnitTest

# Build debug APK
gradle :app:assembleDebug

# Build release APK (configure KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD)
gradle :app:assembleRelease
```

### Installing from GitHub Releases (End Users)
1. Open the repository's **Releases** page (`https://github.com/<OWNER>/BetShield/releases/latest`).
2. Download the attached `.apk` file onto your Android phone.
3. Open the downloaded `.apk` and allow installation from your browser/files app if prompted.
4. Launch **BetShield**, review the onboarding slides, and tap **Activate DNS Shield** to grant local VPN permission.
5. *(Optional)* Enable **App Guard** in Android Accessibility Settings to block installed betting apps.

---

## 5. Permissions Explanation

| Permission | Purpose |
|---|---|
| `BIND_VPN_SERVICE` | Runs local-only DNS filtering (`10.120.0.2:53`) without intercepting HTTPS traffic. |
| `FOREGROUND_SERVICE` / `SPECIAL_USE` | Keeps the local DNS shield reliable while browsing. |
| `INTERNET` & `ACCESS_NETWORK_STATE` | Forwards allowed DNS queries to `1.1.1.1`/`8.8.8.8` and fetches optional HTTPS database updates. |
| `POST_NOTIFICATIONS` *(runtime)* | Displays active shield status and optional blocking alerts on Android 13+. |
| `<queries>` Launcher Intent | Lists installed apps for blocking without requesting `QUERY_ALL_PACKAGES`. |
| `BIND_ACCESSIBILITY_SERVICE` *(opt-in)* | Watches `TYPE_WINDOW_STATE_CHANGED` only (`canRetrieveWindowContent="false"`) to close blocked apps. |

---

## 6. Known Limitations

1. **Private DNS (DoT) & Browser DoH:** If Android Private DNS or Chrome/Firefox Secure DNS is enabled, DNS queries bypass UDP port 53 and the local VPN cannot filter them. Disable Private DNS in Android Settings for full DNS filtering.
2. **Single VPN Slot:** Android allows only one active VPN at a time.
3. **Physical Device Verification:** `VpnService` TUN routing and `AccessibilityService` foreground interception must be verified on a physical Android device or full system emulator image (local JVM Robolectric tests verify DNS packet parsing, matcher rules, database loading, and Compose UI flows).
4. **Release Signing:** GitHub Actions reads `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` from repository secrets. If those secrets are not configured, CI builds an **unsigned** release APK (`app-release-unsigned.apk`) and labels it accordingly rather than claiming a production-signed release.

---

## 7. Contributing

Pull requests adding verified Ethiopian or international gambling domains to `app/src/main/assets/gambling_database.json` are welcome! Please include domain verification evidence and never submit unverified package IDs as verified. See [PRIVACY.md](PRIVACY.md) for security principles.
