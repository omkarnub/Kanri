# Phase 0 Audit: Lend & Borrow Hub

## 1. External Usages of LendingEntity, LendingDao, LendingViewModel, and lending_records
- **HomeViewModel.kt**: Observes `lendingDao.getTotalLentPending(): Flow<Double?>` and `lendingDao.getTotalBorrowedPending(): Flow<Double?>`. Exposes `totalLentPending` and `totalBorrowedPending` in `HomeUiState`.
- **LendingQuickPulse.kt** (Home Pulse Card): Uses `totalLentPending: Double` and `totalBorrowedPending: Double`. Shows `${CurrencyUtils.formatCurrency(totalLentPending)} to collect` (green) and `${CurrencyUtils.formatCurrency(totalBorrowedPending)} you owe` (amber), or `"All settled • No pending dues"`.
- **InsightsViewModel.kt**: Calls `lendingDao.getSplitRecordsBetweenSync(startTime, endTime)`: `List<LendingEntity>`. Filters by `type == "LENT"` and `type == "BORROWED"`.
- **BackupData.kt & BackupRepository.kt**: `BackupPayload.lendingRecords: List<LendingEntity>`. JSON serialization/deserialization with `amount`, `personName`, `type`, `date`, `dueDate`, `isSettled`, `notes`, `linkedTransactionId`. `BackupRepository` reads `getAllRecordsSync()`, restores via `deleteAllRecords()` and `insertAll()`.
- **FactoryResetManager.kt**: Wipes records via `lendingDao.deleteAllRecords()`, then wipes SQLite files via `KanriDatabase.resetDatabase(context)`.
- **MainNavigationScreen.kt**: Instantiates `val lendingViewModel: LendingViewModel = viewModel()`, renders `LendingScreen(viewModel = lendingViewModel)`.

## 2. Home Pulse Card Computation
- Home reads `getTotalLentPending()` (`SELECT SUM(amount) FROM lending_records WHERE type = 'LENT' AND is_settled = 0`) and `getTotalBorrowedPending()` (`SELECT SUM(amount) FROM lending_records WHERE type = 'BORROWED' AND is_settled = 0`).
- The new Hero card directly connects to these identical DAO queries so numbers match by construction.

## 3. Data Types & Return-Date Presets
- `amount`: `Double` (SQLite `REAL NOT NULL`).
- `date`: `Long` epoch millis (SQLite `INTEGER NOT NULL`).
- `return_date` (`dueDate`): `Long?` nullable epoch millis (SQLite `INTEGER DEFAULT NULL`).
- Presets: "No Due Date" (null), "In 7 Days" (+7d), "In 15 Days" (+15d), "In 30 Days" (+30d), and Custom Date.

## 4. Current DB Version, exportSchema, Migrations & SQLCipher
- Version: 8.
- `exportSchema`: `false` in `KanriDatabase.kt`.
- Existing migrations: `MIGRATION_5_6`, `MIGRATION_6_7`, `MIGRATION_7_8`.
- SQLCipher: `System.loadLibrary("sqlcipher")`, `DatabaseKeyManager.getOrCreatePassphrase(appContext)`, `SupportOpenHelperFactory(passphrase)`.

## 5. Serialization, Atomic Restore & Factory Reset
- Handcrafted `org.json.JSONObject` & `org.json.JSONArray` (pure explicit code, R8-safe).
- Atomic restore in `db.withTransaction { ... }`.
- Factory reset calls `deleteAllRecords()` then resets database files.

## 6. Existing Reusable Composables & Transition
- `AnimatedNumberText`: `com.omkarnub.kanri.ui.common.AnimatedNumberText`.
- `formatCurrency`: `com.omkarnub.kanri.util.CurrencyUtils.formatCurrency`.
- Frosted theme: `rememberKanriGlassTheme()` and `LocalHazeState`.
- Slide & fade transition to mirror: `MainNavigationScreen.kt` history transition (`tween(320, FastOutSlowInEasing)`).

## 7. Split Bill Sheet Launch
- `LendingScreen` launches `SplitExpenseSheet(sheetState = splitSheetState, onDismiss = ..., onSaveLenderSplit = ..., onSaveBorrowerSplit = ...)`.
