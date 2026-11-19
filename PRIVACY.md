# BetShield Privacy & Security Specification

BetShield is engineered around **privacy by design**. Helping people overcome gambling habits requires trust, transparency, and zero surveillance.

## What BetShield Never Does
- **No HTTPS / TLS Interception:** BetShield does not install root CA certificates, inspect encrypted payloads, or perform man-in-the-middle (MITM) traffic decryption.
- **No Screen Content Reading:** `BetShieldAccessibilityService` configures `android:canRetrieveWindowContent="false"` and listens exclusively to `TYPE_WINDOW_STATE_CHANGED` foreground package transitions.
- **No Credential or Financial Collection:** BetShield never reads passwords, SMS messages, bank details, telebirr/mobile-money PINs, or gambling logins.
- **No Hidden Telemetry or Cloud Analytics:** All blocking counts, custom domain rules, and settings remain strictly on-device in Room and Jetpack DataStore.

## Network & Permission Transparency
1. **Android `VpnService` (`BIND_VPN_SERVICE`):** Creates a local-only TUN interface (`10.120.0.1/32`) routing only virtual DNS queries (`10.120.0.2:53`). Matched gambling domains receive a local `NXDOMAIN` reply; all other DNS queries are forwarded over a `protect()`ed UDP socket to `1.1.1.1` / `8.8.8.8`.
2. **Optional `AccessibilityService` (`BIND_ACCESSIBILITY_SERVICE`):** Disabled by default. Only active if explicitly enabled by the user in Android Accessibility Settings to return Home when a user-blocked gambling app opens.
3. **Launcher App Query (`<queries>` `ACTION_MAIN` + `CATEGORY_LAUNCHER`):** Avoids the broad `QUERY_ALL_PACKAGES` permission while letting users pick installed apps to restrict.
4. **Optional `POST_NOTIFICATIONS`:** Used on Android 13+ solely for the persistent local VPN status notification and optional blocking alerts.
5. **Optional `INTERNET`:** Used to forward allowed DNS queries while the local VPN is active and when the user explicitly requests a remote JSON registry update over HTTPS.

## Technical Limitations
- **Encrypted DNS (DoT / DoH):** Android System Private DNS (DNS-over-TLS) or browser DNS-over-HTTPS bypasses UDP port 53 local DNS filtering. Users should disable Private DNS in Android Network Settings for web filtering to cover all browsers.
- **Single Active VPN Slot:** Android permits only one active VPN at a time. Enabling another VPN disconnects BetShield's local DNS shield.
- **Unverified Package IDs:** Platforms whose Google Play package identifier cannot be independently verified are marked `verifiedPackage: false` in `gambling_database.json` and require user selection from Installed Apps or manual package ID entry.
