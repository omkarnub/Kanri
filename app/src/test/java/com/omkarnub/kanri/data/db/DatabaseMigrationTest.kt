package com.omkarnub.kanri.data.db

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.StandardCharsets

class DatabaseMigrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDetectsUnencryptedSqliteHeader() {
        val dbFile = tempFolder.newFile("test_plain.db")
        // SQLite format header starts with "SQLite format 3\000"
        val header = "SQLite format 3\u0000SomeOtherDataBytes".toByteArray(StandardCharsets.US_ASCII)
        dbFile.writeBytes(header)

        assertTrue(KanriDatabase.isDatabaseUnencrypted(dbFile))
    }

    @Test
    fun testDetectsEncryptedOrNonSqliteHeader() {
        val dbFile = tempFolder.newFile("test_encrypted.db")
        // Random encrypted ciphertext header
        val header = byteArrayOf(0x5A, 0x12, 0x8F.toByte(), 0x33, 0x44, 0x55, 0x66, 0x77, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88.toByte())
        dbFile.writeBytes(header)

        assertFalse(KanriDatabase.isDatabaseUnencrypted(dbFile))
    }

    @Test
    fun testNonExistentOrEmptyFileReturnsFalse() {
        val nonExistent = File(tempFolder.root, "non_existent.db")
        assertFalse(KanriDatabase.isDatabaseUnencrypted(nonExistent))

        val emptyFile = tempFolder.newFile("empty.db")
        assertFalse(KanriDatabase.isDatabaseUnencrypted(emptyFile))
    }

    @Test
    fun testMigration10_11VersionContract() {
        val migration = KanriDatabase.MIGRATION_10_11
        org.junit.Assert.assertEquals(10, migration.startVersion)
        org.junit.Assert.assertEquals(11, migration.endVersion)
    }

    @Test
    fun testAllMigrationsFormContiguousChain() {
        val chain = listOf(
            KanriDatabase.MIGRATION_5_6 to (5 to 6),
            KanriDatabase.MIGRATION_6_7 to (6 to 7),
            KanriDatabase.MIGRATION_7_8 to (7 to 8),
            KanriDatabase.MIGRATION_8_9 to (8 to 9),
            KanriDatabase.MIGRATION_9_10 to (9 to 10),
            KanriDatabase.MIGRATION_10_11 to (10 to 11),
            KanriDatabase.MIGRATION_11_12 to (11 to 12),
            KanriDatabase.MIGRATION_12_13 to (12 to 13)
        )
        chain.forEach { (migration, expected) ->
            org.junit.Assert.assertEquals(expected.first, migration.startVersion)
            org.junit.Assert.assertEquals(expected.second, migration.endVersion)
        }
    }

    @Test
    fun testMigration12_13CreatesAllExpectedPerformanceIndexes() {
        val executedSql = mutableListOf<String>()
        val dbClass = androidx.sqlite.db.SupportSQLiteDatabase::class.java
        val fakeDb = java.lang.reflect.Proxy.newProxyInstance(
            dbClass.classLoader,
            arrayOf(dbClass)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                else -> null
            }
        } as androidx.sqlite.db.SupportSQLiteDatabase

        KanriDatabase.MIGRATION_12_13.migrate(fakeDb)

        val expectedIndexes = listOf(
            "index_transactions_timestamp",
            "index_transactions_category_id",
            "index_transactions_is_duplicate",
            "index_transactions_needs_review",
            "index_transactions_counterparty",
            "index_transactions_type_timestamp",
            "index_lending_records_is_settled",
            "index_lending_records_linked_transaction_id",
            "index_lending_records_person_name",
            "index_lending_records_date"
        )
        org.junit.Assert.assertEquals(expectedIndexes.size, executedSql.size)
        expectedIndexes.forEach { indexName ->
            assertTrue(
                "Missing CREATE INDEX IF NOT EXISTS statement for $indexName",
                executedSql.any { it.startsWith("CREATE INDEX IF NOT EXISTS $indexName ") }
            )
        }
    }
}
