package com.omkarnub.kanri.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

import androidx.room.migration.Migration

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        CounterpartyCategoryMapEntity::class,
        BudgetEntity::class,
        LendingEntity::class,
        LendingRepaymentEntity::class,
        RecurringPaymentEntity::class,
        SavingsGoalEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class KanriDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun lendingDao(): LendingDao
    abstract fun recurringPaymentDao(): RecurringPaymentDao
    abstract fun savingsGoalDao(): SavingsGoalDao

    companion object {
        @Volatile
        private var INSTANCE: KanriDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN needs_review INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN review_reason TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN notes TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN is_custom INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lending_records ADD COLUMN original_amount REAL DEFAULT NULL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS lending_repayments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        lending_id INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        paid_at INTEGER NOT NULL,
                        note TEXT,
                        FOREIGN KEY(lending_id) REFERENCES lending_records(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lending_repayments_lending_id ON lending_repayments (lending_id)")
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(name = "Auto / Taxi Fare", colorHex = "#F59E0B", iconName = "taxi"),
            CategoryEntity(name = "Food & Dining", colorHex = "#FF7043", iconName = "restaurant"),
            CategoryEntity(name = "Train & Metro", colorHex = "#0284C7", iconName = "train"),
            CategoryEntity(name = "Other", colorHex = "#8D6E63", iconName = "category"),
            CategoryEntity(name = "Groceries", colorHex = "#4CAF50", iconName = "shopping_cart"),
            CategoryEntity(name = "Shopping", colorHex = "#AB47BC", iconName = "shopping_bag"),
            CategoryEntity(name = "Bills & Utilities", colorHex = "#FFA726", iconName = "receipt"),
            CategoryEntity(name = "Transport & Fuel", colorHex = "#29B6F6", iconName = "directions_car"),
            CategoryEntity(name = "Entertainment", colorHex = "#EC4899", iconName = "sports_esports"),
            CategoryEntity(name = "Health & Medical", colorHex = "#EF5350", iconName = "local_hospital"),
            CategoryEntity(name = "Salary & Income", colorHex = "#26A69A", iconName = "payments"),
            CategoryEntity(name = "Investment & Savings", colorHex = "#10B981", iconName = "trending_up"),
            CategoryEntity(name = "Education", colorHex = "#8B5CF6", iconName = "school"),
            CategoryEntity(name = "Cash & ATM", colorHex = "#78909C", iconName = "local_atm"),
            CategoryEntity(name = "Rent & Maintenance", colorHex = "#6366F1", iconName = "home"),
            CategoryEntity(name = "Personal Care", colorHex = "#F43F5E", iconName = "fitness"),
            CategoryEntity(name = "Gifts & Donations", colorHex = "#D946EF", iconName = "gift"),
            CategoryEntity(name = "Travel & Vacation", colorHex = "#06B6D4", iconName = "flight")
        )

        fun getDatabase(context: Context): KanriDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val dbFile = appContext.getDatabasePath("kanri_database")
                migrateUnencryptedDatabaseIfPresent(appContext, dbFile)

                val passphrase = try {
                    DatabaseKeyManager.getOrCreatePassphrase(appContext)
                } catch (e: Throwable) {
                    "kanri_default_fallback_passphrase_32_bytes_len".toByteArray()
                }

                // If an existing database file cannot be decrypted with the current passphrase (e.g. key was rotated or file is corrupted),
                // remove the unopenable files so a fresh database can be created safely without crashing.
                ensureDatabaseCanBeOpened(appContext, dbFile, passphrase)

                val builder = Room.databaseBuilder(
                    appContext,
                    KanriDatabase::class.java,
                    "kanri_database"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedDefaultCategories(appContext)
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            seedDefaultCategories(appContext)
                        }
                    })

                try {
                    System.loadLibrary("sqlcipher")
                    val factory = SupportOpenHelperFactory(passphrase)
                    builder.openHelperFactory(factory)
                } catch (e: Throwable) {
                    Log.w("KanriDatabase", "SQLCipher native factory initialization skipped: ${e.message}")
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        fun ensureDatabaseCanBeOpened(context: Context, dbFile: File, passphrase: ByteArray) {
            if (!dbFile.exists() || dbFile.length() == 0L) return
            try {
                System.loadLibrary("sqlcipher")
                val db = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    passphrase,
                    null,
                    net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
                    null
                )
                try {
                    val cursor = db.rawQuery("SELECT count(*) FROM sqlite_schema", null)
                    cursor.close()
                } finally {
                    db.close()
                }
            } catch (e: Throwable) {
                val msg = e.message ?: ""
                val isEncryptionOrCorruptError = msg.contains("file is not a database") ||
                        msg.contains("code 26") ||
                        msg.contains("passphrase") ||
                        msg.contains("corrupt")
                if (isEncryptionOrCorruptError) {
                    Log.w("KanriDatabase", "Database exists but cannot be decrypted with current key ($msg). Wiping unopenable files.")
                    try {
                        dbFile.delete()
                        val parent = dbFile.parentFile
                        if (parent != null) {
                            File(parent, "${dbFile.name}-wal").delete()
                            File(parent, "${dbFile.name}-shm").delete()
                            File(parent, "${dbFile.name}-journal").delete()
                        }
                        context.deleteDatabase(dbFile.name)
                    } catch (delEx: Throwable) {
                        Log.e("KanriDatabase", "Failed to delete corrupted database file", delEx)
                    }
                } else {
                    Log.w("KanriDatabase", "ensureDatabaseCanBeOpened encountered non-fatal check issue: $msg")
                }
            }
        }

        fun resetDatabase(context: Context) {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (e: Throwable) {
                    Log.e("KanriDatabase", "Error closing database during reset", e)
                }
                INSTANCE = null
                val dbFile = context.getDatabasePath("kanri_database")
                try {
                    if (dbFile.exists()) dbFile.delete()
                    val parent = dbFile.parentFile
                    if (parent != null) {
                        File(parent, "kanri_database-wal").delete()
                        File(parent, "kanri_database-shm").delete()
                        File(parent, "kanri_database-journal").delete()
                    }
                } catch (_: Throwable) {}
                context.deleteDatabase("kanri_database")
            }
        }

        fun isDatabaseUnencrypted(dbFile: File): Boolean {
            if (!dbFile.exists() || dbFile.length() < 16) return false
            val header = ByteArray(16)
            return try {
                FileInputStream(dbFile).use { it.read(header) }
                String(header, StandardCharsets.US_ASCII).startsWith("SQLite format 3")
            } catch (e: Exception) {
                false
            }
        }

        fun migrateUnencryptedDatabaseIfPresent(context: Context, dbFile: File) {
            if (!isDatabaseUnencrypted(dbFile)) return

            try {
                val plainDbFile = File(dbFile.parentFile, "kanri_database_plain.db")
                if (plainDbFile.exists()) {
                    plainDbFile.delete()
                }
                dbFile.renameTo(plainDbFile)

                File(dbFile.parentFile, "kanri_database-wal").delete()
                File(dbFile.parentFile, "kanri_database-shm").delete()

                val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)
                val factory = SupportOpenHelperFactory(passphrase)

                val encryptedDb = Room.databaseBuilder(
                    context,
                    KanriDatabase::class.java,
                    "kanri_database"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()

                encryptedDb.openHelper.writableDatabase.let { db ->
                    val plainPath = plainDbFile.absolutePath.replace("'", "''")
                    db.execSQL("ATTACH DATABASE '$plainPath' AS plaintext KEY ''")
                    val tables = listOf(
                        "categories",
                        "transactions",
                        "counterparty_category_map",
                        "budgets",
                        "lending",
                        "recurring_payments",
                        "savings_goals"
                    )
                    for (table in tables) {
                        try {
                            db.execSQL("INSERT OR REPLACE INTO $table SELECT * FROM plaintext.$table")
                        } catch (e: Exception) {
                            // Table may not exist in earlier DB version, ignore
                        }
                    }
                    db.execSQL("DETACH DATABASE plaintext")
                }
                encryptedDb.close()
                plainDbFile.delete()
            } catch (e: Exception) {
                Log.e("KanriDatabase", "Migration of unencrypted database failed", e)
            }
        }

        private fun seedDefaultCategories(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = getDatabase(context)
                    val catDao = db.categoryDao()

                    // 1. If both "Other" and "Others" exist, merge "Others" into "Other"
                    val allCats = catDao.getAllCategoriesSync()
                    val otherCat = allCats.find { it.name.equals("Other", ignoreCase = true) }
                    val othersCat = allCats.find { it.name.equals("Others", ignoreCase = true) }
                    if (otherCat != null && othersCat != null && otherCat.id != othersCat.id) {
                        db.openHelper.writableDatabase.execSQL(
                            "UPDATE transactions SET category_id = ${otherCat.id} WHERE category_id = ${othersCat.id}"
                        )
                        db.openHelper.writableDatabase.execSQL(
                            "DELETE FROM categories WHERE id = ${othersCat.id}"
                        )
                    } else if (otherCat == null && othersCat != null) {
                        db.openHelper.writableDatabase.execSQL(
                            "UPDATE categories SET name = 'Other' WHERE id = ${othersCat.id}"
                        )
                    }

                    // 2. If both "Transport" and "Transport & Fuel" exist, merge "Transport" into "Transport & Fuel"
                    val transportCat = allCats.find { it.name.equals("Transport", ignoreCase = true) }
                    val transportFuelCat = allCats.find { it.name.equals("Transport & Fuel", ignoreCase = true) }
                    if (transportCat != null && transportFuelCat != null && transportCat.id != transportFuelCat.id) {
                        db.openHelper.writableDatabase.execSQL(
                            "UPDATE transactions SET category_id = ${transportFuelCat.id} WHERE category_id = ${transportCat.id}"
                        )
                        db.openHelper.writableDatabase.execSQL(
                            "DELETE FROM categories WHERE id = ${transportCat.id}"
                        )
                    }

                    // 3. Remove any remaining duplicate categories by lowercase name
                    val currentCats = catDao.getAllCategoriesSync()
                    val seen = mutableSetOf<String>()
                    for (cat in currentCats) {
                        val key = cat.name.trim().lowercase()
                        if (key in seen) {
                            db.openHelper.writableDatabase.execSQL("DELETE FROM categories WHERE id = ${cat.id}")
                        } else {
                            seen.add(key)
                        }
                    }

                    // 4. Insert missing default categories
                    val finalCats = catDao.getAllCategoriesSync()
                    val finalNames = finalCats.map { it.name.lowercase().trim() }.toSet()
                    val toInsert = DEFAULT_CATEGORIES.filter { it.name.lowercase().trim() !in finalNames }
                    if (toInsert.isNotEmpty()) {
                        catDao.insertCategories(toInsert)
                    }
                } catch (e: Throwable) {
                    Log.e("KanriDatabase", "Error seeding/cleaning categories", e)
                }
            }
        }
    }
}
