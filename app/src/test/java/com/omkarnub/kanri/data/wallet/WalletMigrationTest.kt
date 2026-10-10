package com.omkarnub.kanri.data.wallet

import androidx.sqlite.db.SupportSQLiteDatabase
import com.omkarnub.kanri.data.db.KanriDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class WalletMigrationTest {

    @Test
    fun testMigration10To11_executesExpectedSchemaChangesAndBackfill() {
        val executedSql = mutableListOf<String>()

        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        // Execute migration 10 -> 11
        val migration = KanriDatabase.MIGRATION_10_11
        assertEquals(10, migration.startVersion)
        assertEquals(11, migration.endVersion)

        migration.migrate(fakeDb)

        // 1. Verify wallet_balances table creation
        assertTrue(
            "wallet_balances table creation must be executed",
            executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS wallet_balances") && it.contains("wallet_id TEXT PRIMARY KEY NOT NULL") }
        )

        // 2. Verify wallet column addition
        assertTrue(
            "wallet column addition must be executed with default ONLINE",
            executedSql.any { it.contains("ALTER TABLE transactions ADD COLUMN wallet TEXT NOT NULL DEFAULT 'ONLINE'") }
        )

        // 3. Verify transfer_to_wallet column addition
        assertTrue(
            "transfer_to_wallet column addition must be executed",
            executedSql.any { it.contains("ALTER TABLE transactions ADD COLUMN transfer_to_wallet TEXT DEFAULT NULL") }
        )

        // 4. Verify index on transactions(wallet)
        assertTrue(
            "index on transactions(wallet) must be created",
            executedSql.any { it.contains("CREATE INDEX IF NOT EXISTS index_transactions_wallet ON transactions (wallet)") }
        )

        // 5. Verify backfill of CASH transactions
        assertTrue(
            "CASH source_type must be backfilled to CASH wallet",
            executedSql.any { it.contains("UPDATE transactions SET wallet = 'CASH' WHERE source_type = 'CASH'") }
        )

        // 6. Verify backfill of non-CASH transactions
        assertTrue(
            "non-CASH source_type must be backfilled to ONLINE wallet",
            executedSql.any { it.contains("UPDATE transactions SET wallet = 'ONLINE' WHERE source_type != 'CASH'") }
        )
    }
}
