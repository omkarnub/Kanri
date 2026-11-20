# Kanri Website Implementation & Verification Report

**Date:** November 21, 2026  
**Audited Repository:** `https://github.com/omkarnub/Kanri`  
**Package:** `com.omkarnub.kanri` (v1.19.8, Build 75)

---

## 1. List of Pages & Files Built

### Main Pages & Standalone Endpoints
1. **`website/index.html`** (`/`): Official landing page featuring the hero section, real 1440 silent motion loop video, interactive "Try the parser" demo (in-browser regex), feature showcases with alternating real mockups, interactive Bill Splitter and Safe-to-Spend calculators, 4-pillar Financial Health Score simulator (0–100), 12 Pixel DotTiles widgets gallery, live 5-theme switcher (Light, Dark, AMOLED, Glass with real backdrops), security flow diagram, honest permissions table, neutral "Why Kanri" comparison table, supported banks list & disclaimer, download verification box with copy commands, FAQ accordion, and open-source band.
2. **`website/privacy/index.html`** (`/privacy`): Updated official privacy policy matching the real codebase, with an executive plain-language summary box, permissions table, restricted `drive.file` scope disclosures, and GitHub Issues contact.
3. **`website/terms/index.html`** (`/terms`): Plain-language terms of use: GPL-3.0 software provided as-is, non-custodial user backup responsibility, not financial advice, and trademark disclaimer.
4. **`website/download/index.html`** (`/download`): Canonical, dedicated download page featuring release APK links, verified SHA-256 hash, terminal verification commands, and Android 13+ "Allow restricted settings" step-by-step guide.
5. **`website/faq/index.html`** (`/faq`): Full searchable FAQ accordion covering offline mechanics, OTP rejection, supported banks, card & ATM handling, sideloading restricted settings, backups, and encryption.
6. **`website/security/index.html`** (`/security`): Complete security architecture, cryptographic specifications (SQLCipher AES-256, Android Keystore, PBKDF2/AES-GCM), zero-telemetry threat model, and GitHub Security Advisories responsible disclosure policy.
7. **`website/press/index.html`** (`/press`): Official press kit: honest boilerplates, downloadable dark/light logotypes, color tokens, typography rules, 4 device screenshots, and brand usage constraints.
8. **`website/404.html`** (`/404`): Custom on-brand 404 page featuring dot-matrix typography and navigation links back home.

### Documentation Hub (`/docs`)
9. **`website/docs/index.html`**: Hub overview with interactive client-side search and chapter grid.
10. **`website/docs/getting-started/index.html`**: Chapter 1 — APK download, SHA-256 verification, Android 13+ restricted settings, and onboarding.
11. **`website/docs/capture-dedupe/index.html`**: Chapter 2 — Priority 999 SMS receiver, Notification Listener scoping, 5-minute sliding window deduplication, and OTP filtering.
12. **`website/docs/wallets/index.html`**: Chapter 3 — Cash vs. Online wallets, smart ATM withdrawal modes, and Indian lakh/crore numbering.
13. **`website/docs/lend-borrow/index.html`**: Chapter 4 — P2P debt tracking, partial repayment records, one-tap WhatsApp reminders, UPI deep links, and bill splitting (2–20 people).
14. **`website/docs/budgets-goals/index.html`**: Chapter 5 — Custom budget cycles (1–28), dynamic Safe-to-Spend daily allowance, savings goals pacing, and quick steppers.
15. **`website/docs/insights-health-score/index.html`**: Chapter 6 — 18+ visualizers, heatmaps, burn-rate projections, and the 4-pillar 0–100 Financial Health Score engine.
16. **`website/docs/widgets/index.html`**: Chapter 7 — The 12 Pixel DotTiles widgets, Ndot57 font, shared Canvas engine, and battery-safe midnight resets.
17. **`website/docs/themes/index.html`**: Chapter 8 — 5 visual themes, Liquid Glass engine, 22 animated backdrops, AGSL GPU half-precision, and 4 launcher icons.
18. **`website/docs/backup-restore/index.html`**: Chapter 9 — PBKDF2 (65,536 iterations) + AES-GCM encrypted backups (.enc), Google Drive sync, and CSV/JSON/PDF exports.
19. **`website/docs/security-model/index.html`**: Chapter 10 — SQLCipher AES-256, Android Keystore TEE hardware protection, and threat model.
20. **`website/docs/troubleshooting/index.html`**: Chapter 11 — Resolving aggressive OEM battery killers (MIUI/HyperOS, OxygenOS/ColorOS, One UI), watchdog rebinds, and permissions.

### SEO & Configuration Manifests
21. **`website/robots.txt`**: Search crawler indexing directive and sitemap declaration.
22. **`website/sitemap.xml`**: Complete XML sitemap listing all 20 canonical URLs.
23. **`website/llms.txt`**: Factual manifest for AI search engines (Perplexity, ChatGPT Search, Gemini).
24. **`website/SEO_PLAN.md`**: Comprehensive keyword map, repository metadata checklist, and launch strategy.
25. **`website/README.md`**: Web developer guide for previewing and deploying the website.
26. **`.github/workflows/deploy-pages.yml`**: GitHub Actions workflow for automated deployment to GitHub Pages.

---

## 2. Claims & Assets Verification Audit

### A. Google Drive Backup Status (Confirmed)
- **Code Audit:** In `AndroidManifest.xml`, `android.permission.INTERNET` and `ACCESS_NETWORK_STATE` are present. In `CloudBackupManager.kt` and `GoogleDriveService.kt`, Google Drive backup is fully implemented using the restricted `drive.file` scope and Google Drive REST API v3.
- **Action Taken:** In accordance with prompt instructions, all website copy honestly describes Kanri as **"100% on-device ledger with optional client-side encrypted Google Drive backup"**, completely avoiding inaccurate claims of "no internet permission".
- **Confirmation Needed:** Author should confirm if Google Drive OAuth client ID is configured for all build variants.

### B. Contact Email (Configured to GitHub Issues)
- **Code Audit:** `support@kanri.app` is mentioned in the legacy `PRIVACY_POLICY.md`, but domain ownership is unverified.
- **Action Taken:** Made `CONTACT_EMAIL` a config value in `assets/js/config.js` and set its default to `null`. All contact and bug report CTAs across the website point to GitHub Issues (`https://github.com/omkarnub/Kanri/issues`).
- **Confirmation Needed:** Author should confirm whether `support@kanri.app` is an active inbox they own, or whether to continue directing all communication to GitHub Issues.

### C. Play Store Status (Confirmed: Not Live)
- **Code Audit:** No Google Play Store listing exists. The app is distributed directly via GitHub Releases and Orion Store.
- **Action Taken:** `PLAY_STORE_URL` is set to `null` in `assets/js/config.js`. Google Play Store badges are omitted across the site. The download section provides direct APK downloads, verified SHA-256 checksums, and Orion Store links.

---

## 3. Assets, Screenshots & Fonts Used

| Required Asset | Audited Source in Repo | What Was Used on the Website |
| :--- | :--- | :--- |
| **Logotype Font** | `app/src/main/res/font/panchang_bold.otf` | Self-hosted locally in `website/assets/fonts/panchang_bold.otf`. Zero Google Fonts/external CDN calls. |
| **Interface Font** | `app/src/main/res/font/google_sans_*.ttf` | Self-hosted locally in `website/assets/fonts/google_sans_*.ttf`. |
| **Widget Font** | `app/src/main/res/font/ndot57.otf` | Self-hosted locally in `website/assets/fonts/ndot57.otf` for authentic dot-matrix numerals. |
| **Motion Loop** | `kanri-motion/deliverables/` | Encoded and copied `kanri_loop_silent_1440.mp4` (3.4 MB), `kanri_loop.webm` (512 KB), and `poster.png` to `website/assets/media/`. |
| **App Screenshots** | `docs/screenshots/` | Real high-resolution screenshots copied to `website/assets/images/screenshots/` (`home.png`, `insights.png`, `lend-borrow.png`, `goals.png`, `profile.png`). |
| **Widget Previews** | `app/src/main/res/drawable-nodpi/` | 12 real WebP dot-matrix widget renders copied to `website/assets/images/widgets/widget_preview_*.webp`. |
| **Animated Backdrops** | `app/src/main/assets/backgrounds/` | Authentic artwork files (`sumi_e_mist.html`, `aurora.html`, `snowy_mountain_road.html`) copied to `website/assets/backdrops/` for live theme switcher previewing. |

---

## 4. Mismatches Found Between Code and Documentation

1. **"Zero Internet Permission" Claim:**  
   - Earlier project notes and informal descriptions suggested Kanri has zero network permissions.
   - **Fact:** `AndroidManifest.xml` includes `INTERNET` and `ACCESS_NETWORK_STATE` to support optional Google Drive backup (`drive.file` scope) and in-app GitHub release update checking (`AppUpdateManager.kt`).
   - **Resolution:** All newly authored website copy accurately describes Kanri as an on-device ledger with optional client-side encrypted Drive backup. Stale notes in older documentation should be updated.
2. **Version Inconsistency:**  
   - Older docs referenced v1.19.0 and v1.19.7.
   - **Fact:** `app/build.gradle.kts` specifies `versionName = "1.19.8"` and `versionCode = 75`. The release APK on disk is `Kanri-v1.19.8-release.apk` with size `28,418,025` bytes and SHA-256 hash `34DF85A3EAD322E65BC46CB2C7AB30F2D8FBD682289CBA24740C05C3E47C7538`.
   - **Resolution:** All website copy, download sections, and JSON-LD structured data are locked to v1.19.8 (Build 75).

---

## 5. One Recommended Next Step

**Connect GitHub Pages Deployment:**  
Push this commit to GitHub, enable GitHub Pages under **Settings > Pages** in your GitHub repository, and select **GitHub Actions** as the source (a ready-to-run `.github/workflows/deploy-pages.yml` workflow has been included). Once deployed, submit `https://omkarnub.github.io/Kanri/sitemap.xml` to Google Search Console to begin indexing.
