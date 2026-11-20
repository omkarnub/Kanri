/**
 * Kanri Website Configuration
 * Centralized source of truth for honest project facts & links.
 */
window.KANRI_CONFIG = {
  VERSION: "1.19.8",
  BUILD: 75,
  TARGET_SDK: 37,
  MIN_ANDROID: "Android 8.0+ (Oreo, API 26+)",
  RELEASE_DATE: "November 21, 2026",
  
  // Real release APK artifacts
  APK_NAME: "Kanri-v1.19.8-release.apk",
  APK_SIZE: "27.1 MB",
  APK_BYTES: 28418025,
  APK_SHA256: "34DF85A3EAD322E65BC46CB2C7AB30F2D8FBD682289CBA24740C05C3E47C7538",
  
  // URLs & Repositories
  GITHUB_REPO: "https://github.com/omkarnub/Kanri",
  GITHUB_RELEASES: "https://github.com/omkarnub/Kanri/releases",
  GITHUB_ISSUES: "https://github.com/omkarnub/Kanri/issues",
  GITHUB_SECURITY: "https://github.com/omkarnub/Kanri/security/advisories",
  DOWNLOAD_URL: "https://github.com/omkarnub/Kanri/releases/download/v1.19.8/Kanri-v1.19.8-release.apk",
  ORION_STORE_URL: "https://rookieenough.github.io/Orion-Data/redirect.html?id=kanri",
  
  // Non-negotiable honesty flags:
  // Google Play Store URL is strictly null until live.
  PLAY_STORE_URL: null,
  
  // Contact email default: null (unverified domain ownership; routes to GitHub Issues)
  CONTACT_EMAIL: null
};
