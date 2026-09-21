# GuardianX — Personal Safety & Cyber Threat Defense Engine

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Jetpack%20Compose-3DDC84.svg)](https://developer.android.com/jetpack/compose)
[![Language](https://img.shields.io/badge/Language-Kotlin%20Coroutines-7F52FF.svg)](https://kotlinlang.org/)
[![Database](https://img.shields.io/badge/Persistence-SQLite%20Room%20KSP-4285F4.svg)](https://developer.android.com/training/data-storage/room)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%20MVVM%20%2F%20Offline--First-06D6A0.svg)](#architecture)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

GuardianX is an autonomous personal safety, emergency cascade, and cyber threat inspection platform designed for extreme resilience under adverse or disconnected conditions.

---

## Key Features

1. **🚨 Multi-State Emergency Engine (`com.example.emergency`)**:
   * Deterministic state lifecycle: `IDLE` → `ARMED` → `TRIGGERED` → `ACTIVE` → `ESCALATING` → `RESOLVED`.
   * Real hardware haptic countdowns and pulsing tactical SOS animations.
   * Auto-dispatch cascades notifying trusted contacts with real-time GPS coordinates.

2. **📡 100% Offline-First Resilience (`com.example.offline`)**:
   * Full local persistence with Android SQLite Room and TypeConverters.
   * Autonomous sync queue tracking pending uploads under network disconnection.
   * Seamless one-tap or automatic background cloud synchronization upon reconnection.

3. **🔍 Cyber Threat & Phishing Inspection (`com.example.phishing`)**:
   * Multilayered heuristic inspection detecting brand typosquatting homoglyphs (`g00gle`, `paypa1`, etc.).
   * Raw numeric IP hostname evasion detection.
   * Psychological coercion urgency trigger identification in SMS and messaging apps.
   * Flagging of abused disposable high-risk top-level domains.

4. **🔐 Cryptographic Evidence Vault (`com.example.evidence`)**:
   * Tamper-evident forensic records secured by SHA-256 digests.
   * Automatic capture of GPS location trails during distress triggers.
   * Incident note recording with immutable audit logs.

5. **🛡️ Wearable BLE Continuity & Hardware Triggers (`com.example.devices`)**:
   * Pairing and monitoring for external BLE hardware (e.g. ESP32 safety tags).
   * Heartbeat continuity monitors and discreet emergency triggers.

6. **👥 Trusted Emergency Contact Cascade (`com.example.contacts`)**:
   * Priority-ordered contact roster with relationship classifications.
   * Fast emergency dispatch via SMS and background notification channels.

---

## Technical Stack & Design Standards
* **Jetpack Compose & Material Design 3 (M3)** with customized tactical cyberpunk aesthetics (`#0B132B`, `#48CAE4`, `#06D6A0`).
* **Android Architecture Components**: StateFlow, ViewModel, Room DB with KSP, Coroutines, Navigation Compose.
* **Testing**: Comprehensive Robolectric and unit test coverage (`15/15` tests passing).
* **Multi-Platform Deployment Ready**: Google Play Store APK/AAB and static cloud dashboard (`/public/index.html`) deployable to Vercel and Netlify.
