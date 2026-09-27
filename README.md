<p align="center">
  <img src="kanri_app_icon_dark.png" alt="Kanri Logo" width="108" height="108" style="border-radius: 24px;" />
</p>

<h1 align="center">KANRI</h1>

<p align="center">
  <strong>The 100% Offline, Privacy-First Personal Finance Operating System for Android</strong>
</p>

<p align="center">
  <a href="https://github.com/omkarnub/Kanri/releases/tag/v1.0.1"><img src="https://img.shields.io/badge/Release-v1.0.1-000000?style=for-the-badge&logo=android&logoColor=white" alt="Release v1.0.1" /></a>
  <a href="https://github.com/omkarnub/Kanri/releases/download/v1.0.1/Kanri-v1.0.1.apk"><img src="https://img.shields.io/badge/Download-APK%20(24.9%20MB)-30A46C?style=for-the-badge&logo=googleplay&logoColor=white" alt="Download APK" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Privacy-100%25%20Offline-000000?style=for-the-badge&logo=shield&logoColor=white" alt="100% Offline" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Security-SQLCipher%20AES--256-blue?style=for-the-badge" alt="SQLCipher AES-256" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-brightgreen?style=for-the-badge&logo=android" alt="Android 8.0+" /></a>
</p>

---

## ⚡ What is Kanri?

**Kanri** is an intelligent, offline personal financial tracker designed from the ground up for privacy, speed, and tactile elegance. It automatically recognizes bank transactions, card spends, ATM withdrawals, and UPI alerts without sending a single byte of your financial data to any server.

Built entirely with **Kotlin 2.2**, **Jetpack Compose (Material 3)**, and encrypted at rest with **SQLCipher AES-256**.

---

## ✨ Key Features

### 🛡️ 100% Offline & Zero Cloud Leakage
* **Zero Remote Servers:** All database records, categorizations, and notes stay encrypted on your physical device.
* **SQLCipher AES-256:** Database encryption backed by Android Keystore hardware-backed cryptographic keys.
* **Privacy by Default:** Zero tracking SDKs, zero analytics, zero personal loan advertisements, and zero credit score inquiries.

### ⚡ Dual-Engine Transaction Detection
* **High-Precision SMS Parser:** Automatic extraction of transaction amounts, types (Debit/Credit), sources (UPI, ATM, Card, NetBanking), merchants, and UTR/reference numbers across 15+ major banks.
* **Notification Listener:** Captures real-time push receipts from Google Pay, PhonePe, Paytm, CRED, FamPay, Amazon Pay, and BHIM.
* **Chat Privacy Preserved:** Strictly ignores messaging apps (WhatsApp, Telegram, Signal) — only verified banking and payment channels are parsed.
* **Smart Deduplication:** Cross-channel fingerprinting prevents duplicate entries from SMS and push alerts.

### 🖤 Minimalist Luxury Aesthetic
* **Refined Dark Palette:** Tailored `#121212` matte dark background (no harsh AMOLED black crush).
* **Origami 3D FoldText:** Brand typography with staggered perspective hinge unfolding and dynamic crease lighting.
* **Bespoke Typography:** Panchang brand typography paired with Google Sans Flex for data clarity.
* **Frosted Glass Interfaces:** Dynamic backdrop blurring powered by Haze glassmorphism.

### 📳 Intelligent Haptic Engine
* **True Haptics for Advanced Motors:** Hardware-calibrated tactile feedback for devices with linear resonant actuators (LRA / Z-axis linear motors, e.g., Infinix Note 30, Google Pixel, Samsung Galaxy).
* **Whisper-Soft Fallback:** Soft micro-pulses (3ms–8ms) on legacy ERM motors to prevent loud, rattling vibrations.

### 🤝 Lend & Borrow Hub (P2P Credit Ledger)
* Track peer-to-peer debts ("Money I Lent" vs "Money I Borrowed") with a high-contrast net balance ring chart.
* **Partial Repayments:** Log partial installment repayments with remaining balance recalculations.
* **Group Bill Splitter:** Distribute shared expenses dynamically among 2 to 20 people.
* **Native UPI Deep-Links:** 1-tap debt settlement opening Google Pay, PhonePe, or Paytm with pre-filled recipient VPA and amount.
* **Polite WhatsApp Reminders:** Generate contextual, polite payment reminders in one tap.

### 🎯 Savings Milestones Hub
* Target milestone goals with daily and monthly required savings pace formulas.
* Full contribution ledger tracking deposit and withdrawal audit trails.
* Monetary steppers (`+₹500`, `+₹1000`, `+₹2000`, `+₹5000`) for quick allocation.

### 📊 Advanced Insights & Financial Health
* **18+ Analytical Visualizers:** GitHub-style calendar heatmap, 365-day annual spending matrix, month-end projection burn rate, and side-by-side month comparison.
* **Monthly Recap:** Interactive Spotify Wrapped-style visual story deck summarizing monthly financial wins.
* **Financial Health Score:** Proprietary 0–100 algorithm evaluating Savings Rate, Budget Utilization, Spend Velocity, and Debt Burden.

### 📱 Android Ecosystem Integration
* **7 Dedicated Home Screen Widgets:**
  1. *Money Spent Today* (Today's burn + quick add)
  2. *Budget Progress* (Linear safe-to-spend gauge)
  3. *Budget Ring* (Circular donut consumption chart)
  4. *Goals Ring* (Savings milestone tracker)
  5. *Lend & Borrow* (At-a-glance net debt ledger)
  6. *Split Bill* (Quick group split calculator)
  7. *Quick Add* (1-tap floating expense logger)
* **Quick Settings Pull-Down Tile:** Native Android notification shade tile ("Quick Add") for instant logging from any app or lock screen.
* **Screen-Off Auto-Lock:** Automatically locks with biometric authentication when the device screen turns off.

---

## 📥 Installation

Download the latest production release APK:

<p align="center">
  <a href="https://github.com/omkarnub/Kanri/releases/download/v1.0.0/Kanri-v1.0.0.apk">
    <img src="https://img.shields.io/badge/Download-Kanri--v1.0.0.apk-black?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
</p>

* **Package Name:** `com.omkarnub.kanri`
* **Size:** ~24.9 MB *(R8 minified & shrink-wrapped)*
* **Requirements:** Android 8.0 (API 26) or higher

---

## 🛠️ Tech Stack & Architecture

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.2.10 |
| **UI Toolkit** | Jetpack Compose (BOM 2026.02.01), Material 3 |
| **Local Database** | Room 2.8.5 with KSP compiler |
| **Encryption** | SQLCipher 4.6.1 (AES-256), Android Keystore, Google Tink |
| **Background Tasks**| AndroidX WorkManager 2.9.1 |
| **Biometrics** | AndroidX Biometric 1.2.0 |
| **Visual Effects** | Haze Glassmorphism 1.3.1 |
| **Testing** | 35 Unit Test Suites (222+ tests) covering 100% of financial logic |

---

## 🏗️ Building from Source

1. **Clone the repository:**
   ```bash
   git clone https://github.com/omkarnub/Kanri.git
   cd Kanri
   ```

2. **Open in Android Studio:**
   * Recommended: Android Studio Ladybug / Meerkat or later.
   * JDK: Java 17 or Java 21.

3. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run Unit Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 📜 Privacy & Security Manifesto

Kanri was created on the fundamental belief that **your financial data belongs solely to you**.

* **No Analytics:** We do not track user behavior or app usage.
* **No Account Creation:** No email sign-up, phone verification, or passwords required.
* **No Credential Harvesting:** Kanri never requests your banking passwords, ATM PINs, or UPI credentials.
* **Encrypted Backups:** User-initiated backups are encrypted with AES-GCM (PBKDF2) using a master passphrase chosen by you.

Read our full [Privacy Policy](PRIVACY_POLICY.md).

---

<p align="center">
  Crafted with precision by <strong><a href="https://github.com/omkarnub">omkarnub</a></strong>
</p>
