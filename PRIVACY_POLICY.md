# Kanri — Official Privacy Policy

**Effective Date:** September 26, 2026  
**Application ID:** `com.omkarnub.kanri`  
**Architecture:** 100% Offline-First with Optional Client-Side Encrypted Google Drive Backup  

---

## 1. Architectural Philosophy: Offline-First Data Sovereignty

**Kanri** is engineered specifically to give users complete sovereignty over their personal financial records. Kanri **does not operate proprietary database or user account servers**. All transaction logs, category tags, monthly budgets, and personal savings goals are stored solely on your local device.

- **Zero Tracking:** We do not track, log, or profile your financial habits.
- **Zero Third-Party SDKs:** No advertising trackers, no Facebook SDK, no marketing beacons.
- **Zero Telemetry:** No analytics data is broadcast over the internet.

---

## 2. On-Device Data Storage

All data created during your use of Kanri resides in private application sandbox storage utilizing encrypted Room/SQLite database architectures:

- **Transactions:** Timestamp, amount, transaction notes, merchant name, category classification, and payment mode (UPI, Debit/Credit Card, NetBanking, Cash).
- **Budgets:** Monthly spending limits and category budgets.
- **User Profile:** Locally customized display name, optional date of birth, and cached avatar image.
- **Application Preferences:** Theme preference, device lock settings, and custom category rules.

---

## 3. Device Permissions & Transparency

Kanri requests specific Android permissions strictly to support on-device expense tracking features. None of these permissions are used to upload personal data to external servers.

| Permission | Technical Name | Purpose & Scope | Network Transmission |
| :--- | :--- | :--- | :--- |
| **SMS Detection** | `RECEIVE_SMS`<br>`READ_SMS` | Scans incoming bank transactional SMS notifications locally on-device using regex engines to auto-log expenses without manual input. | **None.** Processed purely in memory. Never uploaded. |
| **Instant Overlay** | `SYSTEM_ALERT_WINDOW` | Displays a lightweight floating category chip right after a detected payment so you can assign categories in 1 tap. | **None.** Purely local window manager drawing. |
| **App Security** | `USE_BIOMETRIC`<br>`USE_FINGERPRINT` | Protects app entry using native Android BiometricPrompt hardware security. | **None.** Managed strictly by device Keystore/TEE. |
| **Drive Backup** | `INTERNET` | Communicates directly with Google Drive API when the user explicitly chooses Cloud Backup mode. | Direct TLS connection to Google Drive API only. Zero custom server communication. |

---

## 4. Google Drive Integration & `drive.file` Scope

Kanri provides an optional Cloud Backup mode utilizing your personal Google Drive account.

### The Restricted `drive.file` Scope
Kanri strictly requests `https://www.googleapis.com/auth/drive.file`. This restricted scope guarantees:
- **Isolated Access:** Kanri can **only** read and write the specific encrypted backup file (`kanri_backup.enc`) that Kanri itself creates.
- **Zero Access to Other Files:** Kanri has **no ability** to view, list, read, or modify your personal files, photos, sheets, or documents stored in Google Drive.

### Client-Side Encryption
Before any backup data leaves your device, it is encrypted using client-side encryption. Even when stored in Google Drive, the file cannot be deciphered without your device encryption keys.

### User Control & Disconnection
You can disconnect Google Drive at any time in **Settings > Storage & Backup Mode** by selecting **Fully Offline**, or by revoking access directly in your [Google Account Permissions Dashboard](https://myaccount.google.com/permissions).

---

## 5. Third-Party Libraries & Telemetry

- **No Analytics:** No Google Analytics, Firebase Analytics, Mixpanel, or Segment.
- **No Advertising:** No Google AdMob or ad networks of any kind.
- **Local Diagnostics:** Crash and health logs are retained strictly in internal device cache for the user's manual review and are never automatically dispatched.

---

## 6. Data Deletion & Export Rights

You maintain 100% control over your data:
- **Instant Data Wiping:** Under **Settings > Danger Zone > Reset All Data**, you can instantly delete all local databases, cached images, and preferences.
- **Export Formats:** You can export your full data anytime in open formats: encrypted JSON backups, monthly PDF statements, or CSV spreadsheets.

---

## 7. Contact Information

For any inquiries or technical questions regarding this Privacy Policy:
- **Email:** support@kanri.app
- **Repository:** [https://github.com/omkarnub/Kanri](https://github.com/omkarnub/Kanri)
