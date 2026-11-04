# Changelog

All notable changes to Kanri will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-11-04

### Added
- Redesigned unified **Settings Experience** organized into 8 clear, scannable sections: Appearance & Theme, Wallets & Currency, Security & Privacy, Rules & Categories, Permissions & Detection, Data & Backup, About & Support, and Danger Zone.
- Interactive **Visual Theme Selector** with miniature smartphone preview cards for Light, Dark, AMOLED, Glass, and System modes, including real-time talkback semantics and checkmark indicators.
- Live shader background grid (Aurora, Grain Gradient, Hero Geometric, Prism Gradient, Cloud Shader, Ink Ridges, Fuji Sakura, Sumi-e Mist) that conditionally renders only when Glass mode is active.
- Quick Glass Presets (Default, Ultra Sheer, Obsidian, Luminous, Neon) and exposed previously hidden `Reduce Transparency` accessibility toggle.
- Direct Google Drive Cloud Backup manager dialog with account status, auto-backup toggle, and on-demand synchronization.
- Real-time permission status badges and direct settings intents for SMS Detection, Notification Listener, Notifications, Overlay, and Battery Exemption.
- Reusable, theme-adaptive `SettingsComponents` (`SettingsGroup`, `SettingsRow`, `SettingsSwitchRow`, `SettingsDivider`, `ThemePreviewCard`, `ShaderPreviewCard`, `GlassQuickPresetsRow`, dialogs).
- Comprehensive `SETTINGS_INVENTORY.md` recording all 49 user-configurable preferences, defaults, keys, and verification status.

### Changed
- Strictly decoupled Glass engine and live WebGL backgrounds from standard themes: live backgrounds and glass customizers are now completely excluded from composition in Light, Dark, and AMOLED modes.
- Added lifecycle hooks to `GlassBackgroundView` to explicitly stop and destroy WebGL WebViews when exiting Glass mode, eliminating background CPU/GPU/battery drain.
- Normalized contrast ratios and colors across all themes (Light cream `#FBF8EE`, Dark charcoal `#121212`, AMOLED pitch black `#000000`, and Glass frosted tint) adhering to WCAG AA guidelines.
- Bumped application version to `1.1.0` (Build 4).

### Fixed
- Fixed dead `ThemePreviewCard` code inside `SettingsScreen.kt`.
- Fixed unintended shader activation when selecting live backgrounds in non-glass modes.
- Fixed hardcoded color fallbacks in settings dividers, switches, and currency dialogs that produced low contrast in Light mode.

### Removed
- Removed obsolete, fragmented `ThemesCustomizationScreen.kt`.

## [1.0.2] - 2026-09-27
- Initial release with offline SMS parsing, lending/borrowing ledger, split expenses, and Material 3 design.
